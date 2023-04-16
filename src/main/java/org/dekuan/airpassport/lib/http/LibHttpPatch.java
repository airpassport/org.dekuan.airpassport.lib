package org.dekuan.airpassport.lib.http;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;

import java.io.IOException;

@NoArgsConstructor
@SuperBuilder
@Slf4j
public class LibHttpPatch extends LibHttpEntityRequest
{
	@Override
	public HttpResponseRaw fetchRaw() throws IOException
	{
		return this.fetchRaw( HttpMethod.PATCH );
	}

	@Override
	public String fetchString() throws IOException
	{
		return fetchString( HttpMethod.PATCH );
	}

	@Override
	public HttpModel fetchHttpModel()
	{
		return fetchHttpModel( HttpMethod.PATCH );
	}
}