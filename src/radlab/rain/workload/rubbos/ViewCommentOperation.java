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
import java.util.Map;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import radlab.rain.IScoreboard;


/**
 * View-Comment operation.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class ViewCommentOperation extends RubbosOperation 
{
	public ViewCommentOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);

		this._operationName = "View-Comment";
		this._operationIndex = RubbosGenerator.VIEW_COMMENT_OP;
	}

	@Override
	public void execute() throws Throwable
	{
		StringBuilder response = null;

		// Extract a View-Comment parameters from last response
		String scriptName = this.getGenerator().getViewCommentURL();
		int pos = scriptName.lastIndexOf('/');
		scriptName = scriptName.substring(pos >= 0 ? (pos+1) : 0);
		Map<String,String> params = this.getUtility().findViewCommentParamsInHtml(this.getSessionState().getLastResponse(), scriptName);
		if (params == null
			|| params.isEmpty()
			|| !params.containsKey("commentId")
			|| !params.containsKey("filter")
			|| !params.containsKey("display")
			|| !params.containsKey("storyId")
			|| !params.containsKey("comment_table"))
		{
			//this.getLogger().warning("No valid parameter has been found in the last HTML response. Last response is: " + this.getSessionState().getLastResponse() + ". Operation interrupted.");
			this.setFailed(false);
			this.getGenerator().forceNextOperation(RubbosGenerator.BACK_SPECIAL_OP);
			return;
		}

		URIBuilder uri = new URIBuilder(this.getGenerator().getViewCommentURL());
		uri.setParameter("commentId", params.get("commentId"));
		uri.setParameter("filter", params.get("filter"));
		uri.setParameter("display", params.get("display"));
		uri.setParameter("storyId", params.get("storyId"));
		uri.setParameter("comment_table", params.get("comment_table"));
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
}
