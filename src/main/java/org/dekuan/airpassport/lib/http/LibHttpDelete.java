package org.dekuan.airpassport.lib.http;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@SuperBuilder
@Slf4j
public class LibHttpDelete extends LibHttpBaseRequest
{
	public HttpModel deleteRequest()
	{
		return this.executeRequest( HttpMethod.DELETE );
	}
}
