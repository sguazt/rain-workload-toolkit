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
 * 3) Added note to class doc about the origin of the code.
 */

package radlab.rain.workload.oliojava;

import radlab.rain.IScoreboard;

import java.util.Set;
import java.io.IOException;

/**
 * The TagSearchOperation is an operation that does a tag search. A random tag
 * is generated and the search result page is loaded.
 * <br/>
 * NOTE: Code based on {@code radlab.rain.workload.olio.TagSearchOperation}
 * class and adapted for the Java version of Olio.
 */
public class TagSearchOperation extends OlioOperation 
{
	public TagSearchOperation( boolean interactive, IScoreboard scoreboard )
	{
		super( interactive, scoreboard );
		this._operationName = OlioGenerator.TAG_SEARCH_NAME;
		this._operationIndex = OlioGenerator.TAG_SEARCH;
	}
	
	@Override
	public void execute() throws Throwable
	{
		String tag = RandomUtil.randomTagName( this.getRandomUtil() );

		this.getLogger().finer( "Searching the tag: " + tag );
		String searchUrl = this.getGenerator().tagSearchURL + "?tag=" + tag + "&tagsearchsubmit=Seach+Tags";
		StringBuilder searchResponse = this.getHttpTransport().fetchUrl( searchUrl );
		this.trace( searchUrl );
		if( searchResponse.length() == 0 )
		{
			throw new IOException( "Received empty response" );
		}
		
		this.loadStatics( this.getGenerator().tagSearchStatics );
		this.trace( this.getGenerator().tagSearchStatics );
		
		Set<String> imageUrls = this.parseImages( searchResponse );
		this.loadImages( imageUrls );
		this.trace( imageUrls );
		
		this.setFailed( false );
	}
	
}
