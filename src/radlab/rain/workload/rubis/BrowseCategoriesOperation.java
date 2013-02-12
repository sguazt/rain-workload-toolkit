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

package radlab.rain.workload.rubis;


import java.io.IOException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import radlab.rain.IScoreboard;
import radlab.rain.workload.rubis.model.RubisCategory;


/**
 * Browse-Categories operation.
 *
 * Emulates the following requests:
 * 1. Go to the browse page
 * 2. Click on the 'Browse all items in a category'
 * 3. Emulate a click on a category
 *
 * @author Marco Guazzone (marco.guazzone@gmail.com)
 */
public class BrowseCategoriesOperation extends RubisOperation 
{
	public BrowseCategoriesOperation(boolean interactive, IScoreboard scoreboard) 
	{
		super(interactive, scoreboard);

		this._operationName = "Browse-Categories";
		this._operationIndex = RubisGenerator.BROWSE_CATEGORIES_OP;
	}

	@Override
	public void execute() throws Throwable
	{
		StringBuilder response = null;

		// Go to the browse page
		response = this.getHttpTransport().fetchUrl(this.getGenerator().getBrowseURL());
		this.trace(this.getGenerator().getBrowseURL());
		if (!this.getGenerator().checkHttpResponse(response.toString()))
		{   
			this.getLogger().severe("Problems in performing request to URL: " + this.getGenerator().getBrowseURL() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + "). Server response: " + response);
			throw new IOException("Problems in performing request to URL: " + this.getGenerator().getBrowseURL() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + ")");
		}

		// Emulate a click on the "Browse all items in a category" link
		response = this.getHttpTransport().fetchUrl(this.getGenerator().getBrowseCategoriesURL());
		this.trace(this.getGenerator().getBrowseCategoriesURL());
		if (!this.getGenerator().checkHttpResponse(response.toString()))
		{
			this.getLogger().severe("Problems in performing request to URL: " + this.getGenerator().getBrowseCategoriesURL() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + "). Server response: " + response);
			throw new IOException("Problems in performing request to URL: " + this.getGenerator().getBrowseCategoriesURL() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + ")");
		}

		// Generate a random category
		RubisCategory category = this.getGenerator().generateCategory();
		if (!this.getGenerator().isValidCategory(category))
		{
			// Just print a warning, but do not set the operation as failed
			this.getLogger().warning("No valid category has been found. Operation interrupted.");
			this.setFailed(true);
			return;
		}

		// Emulate a click on a category
		URIBuilder uri = new URIBuilder(this.getGenerator().getSearchItemsByCategoryURL());
		uri.setParameter("category", Integer.toString(category.id));
		uri.setParameter("categoryName", category.name);
		//uri.setParameter("page", Integer.toString(this.getUtility.extractPageFromHTML(this.getLastHTML())));
		uri.setParameter("page", Integer.toString(1));
		uri.setParameter("nbOfItems", Integer.toString(this.getGenerator().getNumItemsPerPage()));
		HttpGet reqGet = new HttpGet(uri.build());
		response = this.getHttpTransport().fetch(reqGet);
		this.trace(reqGet.getURI().toString());
		if (!this.getGenerator().checkHttpResponse(response.toString()))
		{
			this.getLogger().severe("Problems in performing request to URL: " + reqGet.getURI() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + "). Server response: " + response);
			throw new IOException("Problems in performing request to URL: " + reqGet.getURI() + " (HTTP status code: " + this.getHttpTransport().getStatusCode() + ")");
		}

		this.setFailed(false);
	}
}
