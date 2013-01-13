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
 *    3.1) to replace the part to check for event validity by a call to
 *         validateEvent;
 *    3.2) to adapt to the names of form fields used in the Java version of
 *         Olio;
 *    3.2) to adapt to the parameters of the event-detail and add-attendee URLs
 *         used in the Java version of Olio;
 *    3.3) to improve warning messages in case the used cannot attend to the
 *         event.
 * 4) Added note to class doc about the origin of the code.
 */

package radlab.rain.workload.oliojava;

import radlab.rain.IScoreboard;

import java.io.IOException;
import java.util.Set;

/**
 * The EventDetailOperation is an operation that shows the details of an event.
 * The event is chosen at random by parsing the home page for an event ID. The
 * event details page is then loaded. If the user is logged in and can attend
 * the event, the user is added as an attendee 10% of the time.
 * <br/>
 * NOTE: Code based on {@code radlab.rain.workload.olio.EventDetailOperation}
 * class and adapted for the Java version of Olio.
 */
public class EventDetailOperation extends OlioOperation 
{
	public EventDetailOperation( boolean interactive, IScoreboard scoreboard ) 
	{
		super( interactive, scoreboard );
		this._operationName = OlioGenerator.EVENT_DETAIL_NAME;
		this._operationIndex = OlioGenerator.EVENT_DETAIL;
	}
	
	@Override
	public void execute() throws Throwable
	{
		// Select an event by parsing the home page.
		StringBuilder homeResponse = this.getHttpTransport().fetchUrl( this.getGenerator().homepageURL );
		this.trace( this.getGenerator().homepageURL );
		String selectedEvent = RandomUtil.randomEvent( this.getRandomUtil(), homeResponse );
		if ( !this.validateEvent(selectedEvent) ) 
		{
			throw new IOException( "Could not find an event on the home page (" + this.getGenerator().homepageURL + "). Home response: " + homeResponse );
		}
		
		// Make the GET request to load the event details.
		String eventUrl = this.getGenerator().eventDetailURL + "?socialEventID=" + selectedEvent;
		StringBuilder eventResponse = this.getHttpTransport().fetchUrl( eventUrl );
		this.trace( eventUrl );
		if ( eventResponse.length() == 0 )
		{
			throw new IOException( "Received empty response" );
		}
		
		// Load the static files (CSS/JS).
		this.loadStatics( this.getGenerator().eventDetailStatics );
		this.trace( this.getGenerator().eventDetailStatics );
		
		// Load images associated with the event.
		Set<String> imageUrls = parseImages( eventResponse );
		this.loadImages( imageUrls );
		this.trace( imageUrls );
		
		// Check if user can be added as an attendee.
		if ( this.isLoggedOn() )
		{
			boolean canAttend = ( eventResponse.indexOf( "\"Attend\"" ) != -1 );
			boolean isAttending = ( eventResponse.indexOf( "\"Unattend\"" ) != -1 );
			
			if ( canAttend || isAttending )
			{
				// 10% of the time we can add ourselves, we will.
				if ( this.getRandomUtil().random( 0, 9 ) == 0 )
				{
					String attendUrl = this.getGenerator().addAttendeeURL + "?socialEventID=" + selectedEvent + "&userName=" + this.getLoggedUser();
					this.getHttpTransport().fetchUrl( attendUrl  );
					this.trace( attendUrl );
				}
			}
			else
			{
				boolean notFound = eventResponse.indexOf( "This file lives in public/404.html" ) != -1; 
				
				if ( notFound )
				{
					throw new IOException( "404 Error! Page not found: " + eventUrl );
				}
				else
				{
					boolean statusConflict = eventResponse.indexOf("Not logged in.") != -1;
					if (statusConflict)
					{
						this.getLogger().warning("Status conflict: server reported user not logged in.");
					}
					else
					{
						this.getLogger().warning( "Logged on but unable to attend an event: " + canAttend + ", " + isAttending + ". HTTP Response: " + eventResponse );
					}
				}
			}
		}
		
		this.setFailed( false );
	}
}
