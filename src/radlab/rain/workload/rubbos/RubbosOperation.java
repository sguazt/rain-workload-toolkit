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
 * Author: Marco Guazzone (marco.guazzone@gmail.com), 2013.
 */

package radlab.rain.workload.rubbos;


import java.util.Random;
import java.util.logging.Logger;
import radlab.rain.Generator;
import radlab.rain.IScoreboard;
import radlab.rain.LoadProfile;
import radlab.rain.Operation;
import radlab.rain.util.HttpTransport;
import radlab.rain.workload.rubbos.model.RubbosUser;


/**
 * Base class for RUBBoS operations.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public abstract class RubbosOperation extends Operation 
{
	public RubbosOperation(boolean interactive, IScoreboard scoreboard)
	{
		super(interactive, scoreboard);
	}

	@Override
	public void prepare(Generator generator) 
	{
		this._generator = generator;

		LoadProfile currentLoadProfile = generator.getLatestLoadProfile();
		if (currentLoadProfile != null)
		{
			this.setGeneratedDuringProfile(currentLoadProfile);
		}

		String html = this.getSessionState().getLastResponse();
		if (html != null)
		{
			if (html.indexOf("Sorry") != -1)
			{
				// Nothing matched the request, we have to go back
				this.setFailed(true);
			}
		}

		// Reset last-search state if we aren't in a search operation
		if (this._operationIndex != RubbosGenerator.SEARCH_IN_STORIES_OP
			&& this._operationIndex != RubbosGenerator.SEARCH_IN_COMMENTS_OP
			&& this._operationIndex != RubbosGenerator.SEARCH_IN_USERS_OP)
		{
			this.getSessionState().setLastSearchOperation(RubbosUtility.INVALID_STORY_ID);
		}

//		// Select a random user for current session (if needed)
//		RubbosUser loggedUser = this.getGenerator().getUser(this.getSessionState().getLoggedUserId());
//		if (this.getUtility().isAnonymousUser(loggedUser))
//		{
//			loggedUser = this.getGenerator().generateUser();
//			this.getSessionState().setLoggedUserId(loggedUser.id);
//		}
	}

	@Override
	public void postExecute() 
	{
		this.getSessionState().setLastOperation(this._operationIndex);
		if (this.isFailed())
		{
			this.getSessionState().setLastResponse(null);
		}
		else if (this.getSessionState().getLastResponse() != null && this.getSessionState().getLastResponse().indexOf("ERROR") != -1)
		{
			this.getSessionState().setLastResponse(null);
			this.setFailed(true);
		}
	}

	@Override
	public void cleanup()
	{
		// Empty
	}

	public RubbosGenerator getGenerator()
	{
		return (RubbosGenerator) this._generator;
	}

	public HttpTransport getHttpTransport()
	{
		return this.getGenerator().getHttpTransport();
	}

	public Random getRandomGenerator()
	{
		return this.getGenerator().getRandomGenerator();
	}

	public Logger getLogger()
	{
		return this.getGenerator().getLogger();
	}

	public RubbosSessionState getSessionState()
	{
		return this.getGenerator().getSessionState();
	}

	public RubbosUtility getUtility()
	{
		return this.getGenerator().getUtility();
	}

	public RubbosConfiguration getConfiguration()
	{
		return this.getGenerator().getConfiguration();
	}
}