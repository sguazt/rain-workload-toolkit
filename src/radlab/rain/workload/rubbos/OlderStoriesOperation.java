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


import java.io.IOException;
import java.util.Calendar;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import radlab.rain.IScoreboard;


/**
 * Older-Stories operation.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class OlderStoriesOperation extends RubbosOperation 
{
	public OlderStoriesOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);

		this._operationName = "Older-Stories";
		this._operationIndex = RubbosGenerator.OLDER_STORIES_OP;
	}

	@Override
	public void execute() throws Throwable
	{
		StringBuilder response = null;

		int[] date = this.generateDate();

		URIBuilder uri = new URIBuilder(this.getGenerator().getOlderStoriesURL());
		uri.setParameter("day", Integer.toString(date[0]));
		uri.setParameter("month", Integer.toString(date[1]));
		uri.setParameter("year", Integer.toString(date[2]));
		uri.setParameter("page", Integer.toString(this.getUtility().findPageInHtml(this.getSessionState().getLastResponse())));
		uri.setParameter("nbOfStories", Integer.toString(this.getConfiguration().getNumOfStoriesPerPage()));
		HttpGet reqGet = new HttpGet(uri.build());
		response = this.getHttpTransport().fetch(reqGet);
		this.trace(reqGet.getURI().toString());
		if (!this.getGenerator().checkHttpResponse(response.toString()))
		{
			this.getLogger().severe("Problems in performing request to URL: " + reqGet.getURI() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + "). Server response: " + response);
			throw new IOException("Problems in performing request to URL: " + reqGet.getURI() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + ")");
		}

		// Save session data
		this.getSessionState().setLastResponse(response.toString());

		this.setFailed(!this.getUtility().checkRubbosResponse(response.toString()));
	}

	// This method is partly based on edu.rice.rubbos.client.UserSession#computeURLFromState
	private int[] generateDate()
	{
		int day;
		int month;
		int year;

		// We have to make hit on past days more successful than hits on 10 years old stories
		int when = this.getRandomGenerator().nextInt(100);
		if (when > 95)
		{
			// Pickup any date
			day = this.getRandomGenerator().nextInt(31) + 1;
			month = this.getRandomGenerator().nextInt(12) + 1;
			year = this.getConfiguration().getOldestStoryYear()+this.getRandomGenerator().nextInt(3);
		}
		else
		{
			// Some date in the past week
			Calendar cal = Calendar.getInstance();
			day = cal.get(Calendar.DAY_OF_MONTH);
			cal.clear();
			cal.set(this.getConfiguration().getNewestStoryYear(), this.getConfiguration().getNewestStoryMonth(), day);
			when /= 10;
			if (when == 0)
			{
				when = 1;
			}
			else if (when > 7)
			{
				when = 7;
			}
			for (int i = 0; i < when; ++i)
			{
				cal.roll(Calendar.DAY_OF_MONTH, false);
			}
			day = cal.get(Calendar.DAY_OF_MONTH);
			month = cal.get(Calendar.MONTH)+1; // In Calendar class, month start at 0
			year = cal.get(Calendar.YEAR);
		}
		if (year > this.getConfiguration().getNewestStoryYear())
		{
			year = this.getConfiguration().getNewestStoryYear();
		}
		if (year == this.getConfiguration().getOldestStoryYear())
		{
			if (month <= this.getConfiguration().getOldestStoryMonth())
			{
				month = this.getConfiguration().getOldestStoryMonth();
			}
		}

		int[] date = new int[3];
		date[0] = day;
		date[1] = month;
		date[2] = year;

		return date;
	}
}
