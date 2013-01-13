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
 * 3) Added methods getRandomDate and getTags (used in populateEntity).
 * 4) Changed method populateEntity to adapt to the names of form fields used in
 *    the Java version of Olio.
 * 5) Changed method execute:
 *    5.1) to remove part for parsing authentication token
 *         (since it is a Ruby on Rails stuff);
 *    5.2) to add a check on HTTP status code after posting the response;
 *    5.3) to add a warning if the user has not logged in.
 * 6) Added note to class doc about the origin of the code.
 */

package radlab.rain.workload.oliojava;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import radlab.rain.IScoreboard;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.HttpStatus;

/**
 * The AddEventOperation is an operation that creates a new event. The user
 * must be logged on. The creation of the POST involves populating the request
 * with event details, an image, a document, and address data.<br />
 * <br />
 * The requests made include loading the event form, loading the static URLs
 * (CSS/JS), and sending the POST data to the application.
 * <br/>
 * NOTE: Code based on {@code radlab.rain.workload.olio.AddEventOperation} class
 * and adapted for the Java version of Olio.
 */
public class AddEventOperation extends OlioOperation 
{
	private static final int MAX_NUM_TAGS = 7;
	private LinkedHashSet<Integer> tagSet = new LinkedHashSet<Integer>(MAX_NUM_TAGS);
	private StringBuilder tags = new StringBuilder();
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

	public AddEventOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super( interactive, scoreboard );
		this._operationName = OlioGenerator.ADD_EVENT_NAME;
		this._operationIndex = OlioGenerator.ADD_EVENT;
	}
	
	@Override
	public void execute() throws Throwable
	{
		if ( this.isLoggedOn() )
		{
			// Fetch the add event form.
			StringBuilder formResponse = this.getHttpTransport().fetchUrl( this.getGenerator().addEventURL );
			this.trace( this.getGenerator().addEventURL );
			if ( formResponse.length() == 0 )
			{
				throw new IOException( "Received empty response" );
			}
			
			// Load the static files associated with the add event form.
			this.loadStatics( this.getGenerator().addEventStatics );
			this.trace( this.getGenerator().addEventStatics );
			
			// Construct the POST request to create the event.
			HttpPost httpPost = new HttpPost( this.getGenerator().addEventResultURL );
			MultipartEntity entity = new MultipartEntity();
			this.populateEntity( entity );
			httpPost.setEntity( entity );
			
			// Make the POST request.
			StringBuilder postResponse = this.getHttpTransport().fetch( httpPost );
			this.trace( this.getGenerator().addEventResultURL );
			
			// Verify that the request succeeded. 
			int status = this.getHttpTransport().getStatusCode();
			if (HttpStatus.SC_OK != status)
			{
				throw new Exception( "Multipart POST request did not work for URL: " + this.getGenerator().addEventResultURL + ". Returned status code: " + status + "!" );
			}
			int index = postResponse.indexOf( "success" );
			if( index == -1 ) {
				throw new Exception( "Multipart POST request did not work for URL: " + this.getGenerator().addEventResultURL + ". Could not find success message in result body!" );
			}
		}
		else
		{
			// TODO: What's the best way to handle this case?
			this.getLogger().warning( "Login required for " + this._operationName );
		}
		
		this.setFailed( false );
	}
	
	/**
	 * Adds the details and files needed to create a new event in Olio.
	 * 
	 * @param entity        The request entity in which to add event details.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	protected void populateEntity( MultipartEntity entity ) throws UnsupportedEncodingException
	{
		entity.addPart("title", new StringBody( RandomUtil.randomText( this.getRandomUtil(), 15, 20 ) ) );
		entity.addPart("summary", new StringBody( RandomUtil.randomText( this.getRandomUtil(), 50, 100 ) ) );
		entity.addPart("description", new StringBody( RandomUtil.randomText( this.getRandomUtil(), 100, 495 ) ) );
		entity.addPart("telephone", new StringBody( RandomUtil.randomPhone( this.getRandomUtil(), new StringBuilder( 256 ) ) ) );
		String[] date = this.getRandDate();
		entity.addPart("year", new StringBody( date[0] ) ); // year
		entity.addPart("month", new StringBody( date[1] ) ); // month
		entity.addPart("day", new StringBody( date[2] ) ); // day
		entity.addPart("hour", new StringBody( date[3] ) ); // hour
		entity.addPart("minute", new StringBody( date[4] ) ); // minute
		entity.addPart("timezone", new StringBody( RandomUtil.randomTimeZone(this.getRandomUtil()) ) );
		
		// Add uploaded files
		entity.addPart( "upload_event_image", new FileBody(this.getGenerator().eventImg ) );
		entity.addPart( "upload_event_literature", new FileBody( this.getGenerator().eventPdf ) );
		entity.addPart( "tags", new StringBody(this.getTags()));
		
		this.addAddress( entity );

		entity.addPart("submit", new StringBody("Create"));
		entity.addPart("submitter_user_name", new StringBody(UserName.getUserName(this.getRandomUtil().random(1, ScaleFactors.getLoadedUsers()))));
	}

	/**
	 * Create a random datetime.
	 * 
	 * @return The random date as an array of 5 fields: the year, the month,
	 *  the day, the hour, the minute, and the timezone.
	 */
	private String[] getRandDate()
	{
		String[] date = new String[5];

		String strDate = this.dateFormat.format(this.getRandomUtil().makeDateInInterval(BASE_DATE, 0, 540));
		StringTokenizer tk = new StringTokenizer(strDate, "-");
		int counter = 0;
		while (tk.hasMoreTokens())
		{
			date[(counter++)] = tk.nextToken();
		}

		return date;
	}

	/**
	 * Create a string of random tags.
	 *
	 * @return A string containing a space-separated list of tags.
	 *
	 * Note: the number of generated tags is randomly chosen between 1 and
	 * MAX_NUM_TAGS.
	 */
	private String getTags()
	{
		int numTags = this.getRandomUtil().random(1, MAX_NUM_TAGS);
		for (int i = 0; i < numTags; i++)
		{
			this.tagSet.add(RandomUtil.randomTagId(this.getRandomUtil(), 0.1D));
		}
		for (int tagId : this.tagSet)
		{
			//this.tags.append(UserName.getUserName(tagId)).append(' ');
			this.tags.append(RandomUtil.randomTagName(this.getRandomUtil()) + " ");
		}
		this.tags.setLength(this.tags.length() - 1);
		String ret = this.tags.toString();
		this.tags.setLength(0);
		this.tagSet.clear();
		return ret;
	}
}
