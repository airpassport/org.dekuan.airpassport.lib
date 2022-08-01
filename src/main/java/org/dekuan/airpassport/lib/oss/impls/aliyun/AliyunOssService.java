package org.dekuan.airpassport.lib.oss.impls.aliyun;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.dekuan.airpassport.lib.exceptions.AirExceptions;
import org.dekuan.airpassport.lib.oss.OssPropertyLoader;
import org.dekuan.airpassport.lib.oss.OssStorage;
import org.dekuan.airpassport.lib.utils.DeFileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Getter
@Setter
@Slf4j
public class AliyunOssService implements OssStorage
{
	private OssPropertyLoader ossPros;


	@Override
	public void setPropertyLoader( OssPropertyLoader ossPropertyLoader )
	{
		this.ossPros = ossPropertyLoader;
	}

	public UploadReturn uploadFile( String fullFilePath )
	{
		return uploadFile( fullFilePath, null, null, null );
	}

	public UploadReturn uploadFile( String fullFilePath, String customizeSha256, String customizeFileName, String customizeFileExt )
	{
		if ( null == ossPros || ! ossPros.isValid() )
		{
			throw new InvalidParameterException( "invalid properties" );
		}
		if ( null == fullFilePath )
		{
			throw new InvalidParameterException( "invalid fullFilePath" );
		}

		File checkFile = new File( fullFilePath );
		if ( ! checkFile.exists() || checkFile.isDirectory() )
		{
			throw new InvalidParameterException( "file not exists" );
		}
		if ( checkFile.length() > ossPros.getAliyunOss().getMaxFileSize() )
		{
			throw new InvalidParameterException( "file size too big" );
		}

		String fileMimeType = DeFileUtils.detectFileMimeType( checkFile );
		if ( ! ossPros.isSupportedMimeType( fileMimeType ) )
		{
			throw new InvalidParameterException( "unsupported mime type" );
		}

		//	...
		log.info( "==========> Uploading an object to bucket [{}], filename [{}]", ossPros.getAliyunOss().getBucketName(), fullFilePath );

		SimpleDateFormat format	= new SimpleDateFormat( "yyyyMMdd" );
		String today		= format.format( new Date() );
		String fileName;
		String fileExtension;
		String sha256;

		if ( Strings.isNotBlank( customizeSha256 ) &&
			Strings.isNotBlank( customizeFileName ) &&
			Strings.isNotBlank( customizeFileExt ) )
		{
			sha256 = customizeSha256;
			fileName = customizeFileName;
			fileExtension = customizeFileExt;
		}
		else
		{
			sha256 = calculateSha256HexOfFile( fullFilePath );
			fileName = sha256;
			fileExtension = ossPros.getExtensionByMimeType( fileMimeType );
		}
		String newKeyName = String.format( "%s.%s", fileName, fileExtension );

		UploadReturn uploadReturn = UploadReturn.builder()
			.fileHash( sha256 )
			.fileExtension( fileExtension )
			.ossPath( newKeyName )
			.build();

		//	...
		String endpoint		= ossPros.getAliyunOss().getEndpoint();
		String accessKeyId	= ossPros.getAliyunOss().getAccessKeyId();
		String accessKeySecret	= ossPros.getAliyunOss().getAccessKeySecret();
		OSS ossClient = new OSSClientBuilder().build( endpoint, accessKeyId, accessKeySecret );

		try
		{
			if ( ossClient.doesObjectExist( ossPros.getAliyunOss().getBucketName(), newKeyName ) )
			{
				log.info( "🐋🐋🐋 🦀 object already exists: Bucket={}, keyName={}",
					ossPros.getAliyunOss().getBucketName(), newKeyName );
				//	ossClient object will be shut down in finally block
				return uploadReturn;
			}

			//
			//	initialize oss path
			//
			uploadReturn.setOssPath( null );

			//
			//	try to upload new object
			//
			UploadFileRequest uploadFileRequest = new UploadFileRequest(
				ossPros.getAliyunOss().getBucketName(),
				newKeyName );

			// The local file to upload---it must exist.
			uploadFileRequest.setUploadFile( fullFilePath );

			// Sets the concurrent upload task number to 5.
			uploadFileRequest.setTaskNum( 5 );

			// Sets the part size to 1MB.
			uploadFileRequest.setPartSize( 1024 * 1024 );

			// Enables the checkpoint file. By default it's off.
			uploadFileRequest.setEnableCheckpoint( false );

			UploadFileResult uploadResult = ossClient.uploadFile( uploadFileRequest );

			CompleteMultipartUploadResult multipartUploadResult = uploadResult.getMultipartUploadResult();
			log.info( multipartUploadResult.getETag() );

			log.info( "🐋🐋🐋 🍔 object upload successfully: Bucket={}, keyName={}",
				ossPros.getAliyunOss().getBucketName(), newKeyName );

			//
			//	return the new oss url path
			//
			uploadReturn.setOssPath( newKeyName );
		}
		catch ( OSSException oe )
		{
			log.info( "Caught an OSSException, which means your request made it to OSS, "
				+ "but was rejected with an error response for some reason.");
			log.info( "Error Message: " + oe.getErrorMessage() );
			log.info( "Error Code:       " + oe.getErrorCode() );
			log.info( "Request ID:      " + oe.getRequestId() );
			log.info( "Host ID:           " + oe.getHostId() );
		}
		catch ( ClientException ce )
		{
			log.info( "Caught an ClientException, which means the client encountered "
				+ "a serious internal problem while trying to communicate with OSS, "
				+ "such as not being able to access the network." );
			log.info( "Error Message: " + ce.getMessage() );
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
		}
		finally
		{
			ossClient.shutdown();
		}

		return uploadReturn;
	}

	public String downloadToTemporaryFile( String keyName )
	{
		String tempFilename = DeFileUtils.createTemporaryFile( "download-oss" );
		if ( Strings.isBlank( tempFilename ) )
		{
			throw new AirExceptions.Execute( "failed to create temporary file for saving oss contents" );
		}

		if ( this.downloadToFile( keyName, tempFilename ) )
		{
			return tempFilename;
		}

		return null;
	}

	public boolean downloadToFile( String keyName, String targetFullFilename )
	{
		if ( null == ossPros || ! ossPros.isValid() )
		{
			throw new InvalidParameterException( "invalid oss properties" );
		}
		if ( StringUtils.isBlank( keyName ) )
		{
			throw new InvalidParameterException( "invalid keyName" );
		}
		if ( Strings.isBlank( targetFullFilename ) )
		{
			throw new InvalidParameterException( "invalid targetFullFilename" );
		}

		//	...
		log.info( "==========> Downloading an object from bucket [{}], key [{}], to file [{}]",
			ossPros.getAliyunOss().getBucketName(), keyName, targetFullFilename );

		//	...
		OSS client = new OSSClientBuilder().build(
			ossPros.getAliyunOss().getEndpoint(),
			ossPros.getAliyunOss().getAccessKeyId(),
			ossPros.getAliyunOss().getAccessKeySecret()
		);
		try
		{
			//
			//	Download an object from your bucket
			//
			GetObjectRequest getObjectRequest = new GetObjectRequest( ossPros.getAliyunOss().getBucketName(), keyName );
			ObjectMetadata objectMetadata = client.getObject( getObjectRequest, new File( targetFullFilename ) );
			log.info( "==========> Done!" );
			log.info( "==========> Content-Type: " + objectMetadata.getContentType() );

			return true;
		}
		catch ( OSSException oe )
		{
			log.error( "Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
			log.error( "Error Message: " + oe.getErrorMessage() );
			log.error( "Error Code:       " + oe.getErrorCode() );
			log.error( "Request ID:      " + oe.getRequestId() );
			log.error( "Host ID:           " + oe.getHostId() );
		}
		catch ( ClientException ce )
		{
			log.error( "Caught an ClientException, which means the client encountered "
				+ "a serious internal problem while trying to communicate with OSS, "
				+ "such as not being able to access the network." );
			log.error( "Error Message: " + ce.getMessage() );
		}
		finally
		{
			//
			//	Do not forget to shut down the client finally to release all allocated resources.
			//
			client.shutdown();
		}

		return false;
//		OSSObject ossObject = downloadOSSObject( keyName );
//		if ( null != ossObject )
//		{
//			File targetFile = new File( targetFullFilename );
//			FileUtils.copyInputStreamToFile( ossObject.getObjectContent(), targetFile );
//			return true;
//		}
//
//		return false;
	}

	public OSSObject downloadOSSObject( String keyName )
	{
		if ( null == ossPros || ! ossPros.isValid() )
		{
			throw new InvalidParameterException( "invalid oss properties" );
		}
		if ( StringUtils.isBlank( keyName ) )
		{
			throw new InvalidParameterException( "invalid keyName" );
		}

		//	...
		log.info( "==========> Downloading an object from bucket [{}], key [{}]",
			ossPros.getAliyunOss().getBucketName(), keyName );

		OSSObject ossObject = null;
		OSS client = new OSSClientBuilder().build(
				ossPros.getAliyunOss().getEndpoint(),
				ossPros.getAliyunOss().getAccessKeyId(),
				ossPros.getAliyunOss().getAccessKeySecret()
			);
		try
		{
			//
			//	Download an object from your bucket
			//
			GetObjectRequest getObjectRequest = new GetObjectRequest( ossPros.getAliyunOss().getBucketName(), keyName );
			ossObject = client.getObject( getObjectRequest );
			log.info( "==========> Done!" );
			log.info( "==========> Content-Type: " + ossObject.getObjectMetadata().getContentType() );
//			displayTextInputStream( object.getObjectContent() );
		}
		catch ( OSSException oe )
		{
			log.error( "Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
			log.error( "Error Message: " + oe.getErrorMessage() );
			log.error( "Error Code:       " + oe.getErrorCode() );
			log.error( "Request ID:      " + oe.getRequestId() );
			log.error( "Host ID:           " + oe.getHostId() );
		}
		catch ( ClientException ce )
		{
			log.error( "Caught an ClientException, which means the client encountered "
				+ "a serious internal problem while trying to communicate with OSS, "
				+ "such as not being able to access the network." );
			log.error( "Error Message: " + ce.getMessage() );
		}
		finally
		{
			//
			//	Do not forget to shut down the client finally to release all allocated resources.
			//
			client.shutdown();
		}

		return ossObject;
	}

	public String calculateSha256HexOfFile( String fullFilePath )
	{
		if ( Strings.isBlank( fullFilePath ) )
		{
			throw new InvalidParameterException( "invalid fullFilePath" );
		}
		File checkFile = new File( fullFilePath );
		if ( ! checkFile.exists() || checkFile.isDirectory() )
		{
			throw new InvalidParameterException( "file not exists" );
		}
		if ( 0 == checkFile.length() )
		{
			throw new InvalidParameterException( "empty file" );
		}

		try ( FileInputStream is = new FileInputStream( fullFilePath ) )
		{
			//
			//	Calculates the SHA-256 digest and returns the value as a hex string.
			//	https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/digest/DigestUtils.html
			//
			return DigestUtils.sha256Hex( is );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			log.error( e.getMessage() );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}

		return null;
	}
}