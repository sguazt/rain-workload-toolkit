/*
 * Copyright (c) 2010, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  * Neither the name of the University of California, Berkeley
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Modifications by: Marco Guazzone
 * 1) Use of class constants in place of hard-coded values for operation name
 *    and index.
 * 2) Use getter methods to access to attributes of other classes.
 * 3) Changed method execute:
 *    3.1) to remove part for checking user name (this is a Ruby on Rails
 *         stuff);
 *    3.2) to adapt to the names of form fields used in the Java version of
 *         Olio;
 *    3.3) to add a check on HTTP status code after posting the response.
 * 4) Changed method populateEntity to adapt to the names of form fields used i
 *    the Java version of Olio.
 * 5) Added note to class doc about the origin of the code.
 */

package radlab.rain.workload.oliojava;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.HttpStatus;

import radlab.rain.IScoreboard;

/**
 * The AddPersonOperation is an operation that creates a new user. If the user
 * is logged in, the session is first logged out. The creation of the user
 * involves obtaining a new user ID (via a synchronized counter), generating
 * a unique username (uniqueness is checked via a name checking request), and
 * creating and executing the POST request with all the necessary user details.
 * <br/>
 * NOTE: Code based on {@code radlab.rain.workload.olio.AddPersonOperation}
 * class and adapted for the Java version of Olio.
 */
public class AddPersonOperation extends OlioOperation 
{
	public AddPersonOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super( interactive, scoreboard );
		this._operationName = OlioGenerator.ADD_PERSON_NAME;
		this._operationIndex = OlioGenerator.ADD_PERSON;
		
		/* Logging in cannot occur asynchronously because the state of the
		 * HTTP client changes, affecting the execution of the following
		 * operation. */
		this._mustBeSync = true;
	}
	
	@Override
	public void execute() throws Throwable
	{
		if ( this.isLoggedOn() )
		{
			this.logOff();
		}
		
		// Fetch the new user form.
		this.getHttpTransport().fetchUrl( this.getGenerator().addPersonURL );
		this.trace( this.getGenerator().addPersonURL );
		
		// Load the static files associated with the new user form.
		this.loadStatics( this.getGenerator().addPersonStatics );
		this.trace( this.getGenerator().addPersonStatics );
		
		// Decide on a user ID and username.
		long id = this.generateUserId();
		String username = UserName.getUserName(id);
		if (username == null || username.length() == 0)
		{
			this.getLogger().warning( "Username is null!" );
		}
		String password = String.valueOf( id );
		
		// Construct the POST request to create the user.
		HttpPost httpPost = new HttpPost( this.getGenerator().fileUploadPersonURL );
		MultipartEntity entity = new MultipartEntity();
		entity.addPart( "user_name", new StringBody( username ) );
		entity.addPart( "password", new StringBody( password ) );
		entity.addPart( "passwordx", new StringBody( password ) );
		entity.addPart( "email", new StringBody( username + "@" + this.getRandomUtil().makeCString( 3, 10 ) + ".com") );
		entity.addPart( "Submit", new StringBody("Create") );
		this.populateEntity( entity );
		httpPost.setEntity( entity );
		
		// Make the POST request and verify that it succeeds.
		this.getHttpTransport().fetch( httpPost );
		this.trace( this.getGenerator().fileUploadPersonURL );
		
		int status = this.getHttpTransport().getStatusCode();
		if ( HttpStatus.SC_OK != status )
		{
			throw new IOException("Multipart POST did not work for URL: " + this.getGenerator().fileUploadPersonURL + ". Returned status code: " + status + "!");
		}

		this.logOn();
		
		this.setFailed( false );
	}
	
	/**
	 * Adds the details and images needed to create a new user.
	 * 
	 * @param entity        The request entity in which to add the details.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	protected void populateEntity( MultipartEntity entity ) throws UnsupportedEncodingException
	{
		StringBuilder buffer = new StringBuilder( 256 );
		
		StringBody firstName = new StringBody( RandomUtil.randomName( this.getRandomUtil(), buffer, 2, 12 ).toString() );
		buffer.setLength( 0 );
		
		StringBody lastName  = new StringBody( RandomUtil.randomName( this.getRandomUtil(), buffer, 5, 12 ).toString() );
		buffer.setLength( 0 );
		
		StringBody telephone = new StringBody( RandomUtil.randomPhone( this.getRandomUtil(), buffer ).toString() );
		
		entity.addPart( "first_name", firstName );
		entity.addPart( "last_name", lastName );
		entity.addPart( "telephone", telephone );
		entity.addPart( "summary", new StringBody( RandomUtil.randomText( this.getRandomUtil(), 50, 200 ) ) );
		entity.addPart( "timezone", new StringBody( RandomUtil.randomTimeZone( this.getRandomUtil() ) ) );
		
		// Add image for person
		entity.addPart( "upload_person_image", new FileBody( this.getGenerator().personImg ) );
		
		this.addAddress( entity );
	}
	
}
