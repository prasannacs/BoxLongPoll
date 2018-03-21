package com.box.longpoll;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BoxAPIFactory {
	
	final String CURRENT_STREAM_URI = "https://api.box.com/2.0/events?stream_position=now";
	final String EVENT_URI = "https://api.box.com/2.0/events";
	
	public String getCurrentStreamPosition(String token) throws Exception	{
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getBoxAPIResponse(new URI(CURRENT_STREAM_URI),token, HttpMethod.GET));
        JsonNode next_stream_position = root.path("next_stream_position");
//        System.out.println("getCurrentStreamPosition -- next_stream_position -- "+next_stream_position.asText());
        return next_stream_position.asText();
	}
	
	private String getBoxAPIResponse(URI uri, String token, HttpMethod method) throws Exception	{
		RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
       	headers.set("Authorization", "Bearer "+token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> response;
        try	{
        response = restTemplate.exchange(uri, method, entity, String.class);
        }catch(HttpClientErrorException clientExp) {
        	throw new Exception("Not a valid Box token. Please try refereshing the token and make sure to pass token argument as token=<your_token> "+clientExp.getMessage());
        }
        if( response == null )
        	throw new Exception("Box API returned invalid response");
        return response.getBody();
	}
	
	
	public String getLongPollURL(String token) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getBoxAPIResponse(new URI(EVENT_URI),token,HttpMethod.OPTIONS));
        JsonNode entries = root.path("entries");
        JsonNode url = null;
        if (entries.isArray()) {
            for (final JsonNode objNode : entries) {
                url = objNode.path("url");
            }
        }
        if(url == null )
        	throw new Exception("No valid Long Poll URL returned from Box API");
        System.out.println("realtime url: "+url.asText());
        return url.asText();
	}
	
	@Async
	public CompletableFuture<String> startLongPoll(String stream_position, String uri, String token)	throws Exception {
		String url = uri + "&stream_position="+stream_position;
		System.out.println("long polling...");
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode message = root.path("message");
        if(message == null )
        	throw new Exception("No valid long poll output message returned from Box");
		return CompletableFuture.completedFuture(message.asText());
	}
	
	public String getEventDetails(String stream_position,String token) throws Exception	{
		String url = EVENT_URI + "?stream_position="+stream_position;
		ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getBoxAPIResponse(new URI(url),token,HttpMethod.GET));
        JsonNode entries = root.path("entries");
        JsonNode eventType = null;
        JsonNode eventId = null;
        if (entries.isArray()) {
            for (final JsonNode objNode : entries) {
            	eventId = objNode.path("event_id");
            	eventType = objNode.path("event_type");
            }
        }
        if(eventId == null || eventType == null)
        	throw new Exception("No valid event details returned from Box");
        return eventId.asText() + " | " + eventType.asText();
	}

	/*
	public String startLongPoll(String stream_position, String uri, String token)	throws Exception {
		String url = uri + "&stream_position="+stream_position;
		System.out.println("Long poll URL -- "+url);
		AsyncRestTemplate restTemplate = new AsyncRestTemplate();
		DeferredResult<String> result = new DeferredResult<>();
		ListenableFuture<ResponseEntity<String>> futureEntity = restTemplate.getForEntity(url, String.class);
		
	    futureEntity.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {

	        @Override
	        public void onFailure(Throwable ex) {
	            result.setErrorResult(ex.getMessage());
	        }

			@Override
			public void onSuccess(ResponseEntity<String> response) {
				System.out.println("Sucesss "+response);
				result.setResult(response.getBody());
			}
	    });
	    System.out.println("result "+result.getResult());
	    return result.getResult().toString();		
	}*/

}
