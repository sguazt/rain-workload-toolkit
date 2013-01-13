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
 * 1) Removed attributes:
 *    - _http,
 *    - _random,
 *    - _logger,
 *    - _isLoggedOn,
 *    - _loggedUser;
 * 2) Added attributes:
 *    - _cachedHeaders,
 *    - BASE_DATE.
 * 3) Changed method fileService to adapt to the images URLs used in the Java
 *    version of Olio.
 * 4) Added methods:
 *    - setIsLoggedOn,
 *    - getLoggedUser,
 *    - setLoggedUser,
 *    - getHttpTransport,
 *    - getRandomUtil,
 *    - getLogger,
 *    - validateEvent.
 * 5) Removed methods:
 *    - parseAuthToken.
 * 6) Use getter methods to access to attributes of other classes.
 * 7) Changed method logOn:
 *    7.1) to adapt to the parameters of the URLs used in the Java version of
 *         Olio;
 *    7.2) to add a check for the login operation.
 * 8) Changed method addAddress to adapt to form parameters used in the Java
 *    version of Olio.
 * 9) Added note to class doc about the origin of the code.
 */

package radlab.rain.workload.oliojava;

import radlab.rain.Generator;
import radlab.rain.IScoreboard;
import radlab.rain.LoadProfile;
import radlab.rain.Operation;

import radlab.rain.util.HttpTransport;
import radlab.rain.workload.oliojava.Random;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The OlioOperation class contains common static methods for use by the
 * operations that inherit from this abstract class.<br />
 * <br />
 * When an operation executes, it should:
 * <ol>
 *   <li>Update the number of actions performed (e.g. one operation may issue
 *	     multiple HTTP requests).</li>
 *   <li>Record the requests issued in the order they were performed so a trace
 *	     can be recreated at a later time. The actual recreation of the trace
 *	     is done by the Scoreboard.</li>
 * </ol>
 * <br/>
 * NOTE: Code based on {@code radlab.rain.workload.olio.OlioOperation}
 * class and adapted for the Java version of Olio.
 */
public abstract class OlioOperation extends Operation 
{
	protected int _imagesLoaded = 0;
	protected long _imgBytes = 0;
	
	// Runtime variables that need to be set by the Generator
	protected HashSet<String> _cachedURLs = new HashSet<String>();
	protected boolean _isCached = false;
	private Map<String, String> _cachedHeaders = new LinkedHashMap<String,String>();
	
	public static final Date BASE_DATE = new Date(System.currentTimeMillis());

	/**
	 * Returns the OlioGenerator that created this operation.
	 * 
	 * @return      The OlioGenerator that created this operation.
	 */
	public OlioGenerator getGenerator()
	{
		return (OlioGenerator) this._generator;
	}
	
	public OlioOperation( boolean interactive, IScoreboard scoreboard )
	{
		super( interactive, scoreboard );
	}
	
	@Override
	public void prepare(Generator generator) 
	{
		this._generator = generator;
		OlioGenerator olioGenerator = (OlioGenerator) generator;
		
		LoadProfile currentLoadProfile = generator.getLatestLoadProfile();
		if( currentLoadProfile != null )
			this.setGeneratedDuringProfile( currentLoadProfile );
		
		// TODO: Should these values be cloned instead of assigned? (e.g. asynchronous operations)
		
		// Refresh the cache to simulate real-world browsing.
		this.refreshCache();
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		this._cachedHeaders.put("If-Modified-Since", sdf.format(BASE_DATE));
	}
	
	@Override
	public void cleanup()
	{
		
	}
	
	/**
	 * Parses an HTML document for image URLs specified by IMG tags.
	 * 
	 * @param buffer    The HTTP response; expected to be an HTML document.
	 * @return          An unordered set of image URLs.
	 */
	public Set<String> parseImages( StringBuilder buffer ) 
	{
		LinkedHashSet<String> urlSet = new LinkedHashSet<String>();
		
		String regex = "<img[^>]*src=\"([^\"]+)\"[^>]*>";
		Pattern pattern = Pattern.compile( regex, Pattern.DOTALL | Pattern.UNIX_LINES );
		Matcher match = pattern.matcher( buffer );
		
		this.getLogger().finest( "Parsing images from buffer" );
		while ( match.find() )
		{
			String link = match.group( 1 );
			if ( link.startsWith(OlioGenerator.CONTEXT_ROOT + "/access-artifacts") && link.contains("jpg") )
			{
				String url = this.getGenerator().hostURL + "/" + link;
				this.getLogger().finest( "Adding " + url );
				urlSet.add(url);
			}
		}
		
		return urlSet;
	}
	
	/**
	 * Load the image files specified by the image URLs if they were not
	 * previously loaded and cached.
	 * 
	 * @param imageURLs     The set of image URLs.
	 * @return              The number of images loaded.
	 * 
	 * @throws IOException
	 */
	protected long loadImages( Set<String> imageUrls ) throws IOException 
	{
		long imagesLoaded = 0;
		
		if ( imageUrls != null )
		{
			for (String imageUrl : imageUrls )
			{
				// Do not load if cached (adding returns false if present).
				if ( this._cachedURLs.add( imageUrl ) )
				{
					this.getLogger().finer( "Loading image: " + imageUrl );
					this.getHttpTransport().fetchUrl( imageUrl );
					this._imgBytes += this.getHttpTransport().getResponseLength();
					imagesLoaded++;
				} 
				else
				{
					this.getLogger().finer( "Image already cached: " + imageUrl );
				}
			}
		}
		return imagesLoaded;
	}
	
	/**
	 * Load the static files specified by the URLs if the current request is
	 * not cached and the file was not previously loaded and cached.
	 * 
	 * @param urls      The set of static file URLs.
	 * @return          The number of static files loaded.
	 * 
	 * @throws IOException
	 */
	protected long loadStatics( String[] urls ) throws IOException 
	{
		long staticsLoaded = 0;
		
		for ( String url : urls )
		{
			if ( this._cachedURLs.add( url ) ) 
			{
				this.getLogger().finer( "Loading URL: " + url );
				this.getHttpTransport().fetchUrl( url );
				staticsLoaded++;
			}
			else 
			{
				this.getLogger().finer( "URL already cached: " + url );
				this.getHttpTransport().fetchUrl(url, this._cachedHeaders);
			}
		}
		
		return staticsLoaded;
	}
	
	/**
	 * Checks if the current HTTP client session is logged in or not. This
	 * should only be used for debugging purposes.
	 * 
	 * @return      True if the user is logged in.
	 */
	public boolean checkIsLoggedIn()
	{
		boolean loggedIn = false;
		
		try
		{
			StringBuilder responseBuffer = this.getHttpTransport().fetchUrl( this.getGenerator().homepageURL );
			loggedIn = responseBuffer.indexOf( "Username" ) == -1;
		}
		catch ( Exception e )
		{}
		
		return loggedIn;
	}
	
	/**
	 * Returns the current "logged on" status.
	 * 
	 * @return      True if this user is "logged on"; otherwise false.
	 */
	protected boolean isLoggedOn() {
		return this.getGenerator().isLoggedOn();
	}
	
	protected void setIsLoggedOn( boolean val )
	{
		this.getGenerator().setIsLoggedOn( val );
	}

	protected String getLoggedUser()
	{
		return this.getGenerator().getLoggedUser();
	}

	protected void setLoggedUser( String user )
	{
		this.getGenerator().setLoggedUser( user );
	}

	protected HttpTransport getHttpTransport()
	{
		return this.getGenerator().getHttpTransport();
	}

	protected Random getRandomUtil()
	{
		return this.getGenerator().getOlioRandomUtil();
	}

	protected Logger getLogger()
	{
		return this.getGenerator().getLogger();
	}

	/**
	 * Executes an HTTP request to log on. This method is not thread-safe and
	 * must not be called by an asynchronously executed operation. Any headers
	 * indicating a redirect will be executed, and the response of the last
	 * request is returned.
	 * 
	 * @return      The content of the response.
	 * 
	 * @throws IOException 
	 */
	protected StringBuilder logOn() throws IOException 
	{
		if ( !this._mustBeSync )
		{
			this.getLogger().warning( "logOn() is not thread-safe and should only" +
					"be called by a synchronous operation!" );
		}
		
		this.getLogger().finer( "Logging on. (Currently: " + this.isLoggedOn() + ")" );
		
		// Decide on the username and password.
		int userId = this.selectUserID();
		String username = UserName.getUserName( userId );
		String password = String.valueOf( userId );
		this.getLogger().fine( "Logging on as " + username + " (ID/Password: " + password + ")" );
		
		// Make the POST request to log in.
		String postBody = "user_name=" + username + "&password=" + password;
		StringBuilder response = this.getHttpTransport().fetchUrl( this.getGenerator().loginURL, postBody, this.getGenerator().loginHeaders );
		this.trace( this.getGenerator().loginURL );
		
		int status = this.getHttpTransport().getStatusCode();
		if (HttpStatus.SC_OK != status)
		{
			throw new IOException("Login as " + username + ", " + userId + " failed. Returned status code: " + status);
		}
		int idx = response.indexOf("Username");
		if (idx != -1)
		{
			throw new IOException("Found login prompt at index " + idx + ", Login as " + username + ", " + userId + " failed.");
		}
        // Check that the user was successfully logged in.
		idx = response.indexOf("Successfully logged in");
		if (idx == -1)
		{
			throw new IOException( "Login did not persist for an unknown reason. Home response: " + response );
		}

		this.setIsLoggedOn( true );
		this.setLoggedUser( username );
		
		return response;
	}
	
	/**
	 * Executes an HTTP request to log off. This method is not thread-safe and
	 * must not be called by an asynchronously executed operation. Any headers
	 * indicating a redirect will be executed, and the response of the last
	 * request is returned.
	 * 
	 * @return      The content of the response.
	 * 
	 * @throws IOException
	 */
	protected StringBuilder logOff() throws IOException 
	{
		if ( !this._mustBeSync )
		{
			this.getLogger().warning( "logOff() is not thread-safe and should only" +
					"be called by a synchronous operation!" );
		}
		
		this.getLogger().finer( "Logging off. (Currently: " + this.isLoggedOn() + ")" );
		StringBuilder response = this.getHttpTransport().fetchUrl( this.getGenerator().logoutURL );
		
		this.setIsLoggedOn( false );
		this.setLoggedUser( "" );
		
		return response;
	}
	
	/**
	 * Refreshes the cache by resetting it 40% of the time.
	 * 
	 * @return      True if the cache was refreshed; false otherwise.
	 */
	protected boolean refreshCache()
	{
		boolean resetCache = ( this.getRandomUtil().random( 0, 9 ) < 6 ); 
		if ( resetCache )
		{
			this._cachedURLs.clear();
		}
		
		return resetCache;
	}
	
	/**
	 * Generates a new user ID (via a synchronized counter). This is used to
	 * create a new user.
	 * 
 	 * @return      A new user ID.
	 */
	protected long generateUserId() throws UnsupportedEncodingException
	{
		long addedUsers = this.getGenerator()._personsAdded.addAndGet( 1L ) * ScaleFactors.activeUsers;
		
		return ScaleFactors.loadedUsers + addedUsers + this.getGeneratorThreadID() + 1;
	}
	
	/**
	 * Used to decide on a random user ID.
	 * 
	 * @return      Returns an integer representing a valid user ID.
	 */
	protected int selectUserID()
	{
		return this.getRandomUtil().random( 0, 3 ) * ScaleFactors.activeUsers + (int) this.getGeneratorThreadID() + 1;
	}
	
	/**
	 * Adds necessary address fields to a multipart request entity.
	 * 
	 * @param reqEntity     The multipart request entity to add fields to.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public void addAddress( MultipartEntity reqEntity ) throws UnsupportedEncodingException
	{
		reqEntity.addPart( "street1", new StringBody( street1() ) );
		reqEntity.addPart( "street2", new StringBody( street2() ) );
		reqEntity.addPart( "city",    new StringBody( this.getRandomUtil().makeCString( 4, 14 ) ) );
		reqEntity.addPart( "state",   new StringBody( this.getRandomUtil().makeCString( 2, 2 ).toUpperCase() ) );
		reqEntity.addPart( "zip",     new StringBody( this.getRandomUtil().makeNString( 5, 5 ) ) );
		reqEntity.addPart( "country", new StringBody( country() ) );
	}
	
	/**
	 * Generates the first line of a pseudorandom street address.
	 * 
	 * @return      A random street address.
	 */
	public String street1() 
	{
		StringBuilder buffer = new StringBuilder( 255 );
		buffer.append( this.getRandomUtil().makeNString( 1, 5 ) ).append( ' ' ); // Number
		RandomUtil.randomName( this.getRandomUtil(), buffer, 1, 11 ); // Street Name
		
		String[] STREETEXTS = { "Blvd", "Ave", "St", "Ln", "" };
		String streetExt = STREETEXTS[this.getRandomUtil().random( 0, STREETEXTS.length - 1 )];
		if ( streetExt.length() > 0 )
		{
			buffer.append( ' ' ).append( streetExt );
		}
		
		return buffer.toString();
	}
	
	/**
	 * Generates the second line of a pseudorandom street address.
	 * 
	 * @return      Half the time, a second line of a random street address;
	 *              otherwise an empty string.
	 */
	public String street2() 
	{
		String street = "";
		
		int toggle = this.getRandomUtil().random( 0, 1 );
		if ( toggle > 0 )
		{
			street = this.getRandomUtil().makeCString( 5, 20 );
		}
		
		return street;
	}
	
	/**
	 * Generates a pseudorandom country.
	 * 
	 * @return      Half the time, USA; otherwise, a random string.
	 */
	public String country() 
	{
		String country = "USA";
		
		int toggle = this.getRandomUtil().random( 0, 1 );
		if ( toggle == 0 ) 
		{
			StringBuilder buffer = new StringBuilder( 255 );
			country = RandomUtil.randomName( this.getRandomUtil(), buffer, 6, 16 ).toString();
		}
		
		return country;
	}

	// Taken for apache.olio.workload.driver.UIDriver
	protected boolean validateEvent(String event)
	{
		return event != null && event.length() != 0 && !event.equals("-1");
	}

}
