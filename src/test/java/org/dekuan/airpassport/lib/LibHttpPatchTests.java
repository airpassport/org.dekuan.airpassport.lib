package org.dekuan.airpassport.lib;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LibHttpPatchTests
{
	final String jwtToken	= "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJhdXRoIiwiZXhwIjoxNjU2Mjc2NzAxLCJ1c2VySWQiOjMzLCJpYXQiOjE2NTM2ODQ3MDEsImp0aSI6IjViNTUwOWRlLTQ5MGItNDIxZS05OWM3LTFlMzIxYWU2MzBjMyJ9.YxNt-emUohSq0-oEzxJNK9TqcgheH-9Y_NposLv8RoI97uki2FDU66wKNSGcaodHRmPgL8XJt_trT4VXwPg1DeXuKFppVQYJ0RweabXpum8ON5KYIAxvru0fOSH_pfiI8X-FOxV_vZaT7DuHus7SFNbjFmPtetukA2Q4NvSMGmschS76jnbkwMc38E3sVjzSfFgU2Y-ul5UATTV2OevgUXFHZ9dQvaP3G_b1hQTgsPrpH3A-pODs6YRAIQPt-BJRqFudB2fP5276ar1fODLBx4-KTHfLSsqbQWmlz28W8JEPHzodTyCBI0tjBrpBiMoxgfvfbO1IhtmMpSrg_rikNw";

	@Test
	public void testSchedulePatchLockingMails()
	{
//		String url = "http://localhost:2022/api/v1/schedule/task/locking/mails/";
//		HashMap<String,Object> postData = new HashMap<>();
//		postData.put( "maxMail", 1 );
//
//		LibHttpPatch libHttp = LibHttpPatch.builder()
//			.url( url )
//			.contentType( LibHttp.HttpContentType.ApplicationJson )
//			.postData( postData )
//			.auth( LibHttp.Auth.builder()
//				.authType( LibHttp.Auth.AuthType.Bearer )
//				.bearerToken( jwtToken )
//				.build() )
//			.header( LibHttp.Header.builder()
//				.xTenantIDValue( "1" )
//				.build() )
//			.build();
//		HttpModel httpModel = libHttp.fetchHttpModel();
//		log.debug( "httpModel : {}", httpModel.toString() );
//
//		assertTrue( httpModel.getStatus() >= 200 );
	}
}
