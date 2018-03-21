package com.box.longpoll;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BoxLongPollApplication {

	/*
	 * @param - Box Developer Token as program argument
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(BoxLongPollApplication.class, args);
		if( args[0] == null )	{
			System.out.println("Please feed in an Authorization Developer Token from Box ..");
			System.exit(0);
		}
		String token = args[0].substring(6);
		System.out.println("Authorization Token -- "+token);
		callBoxAPI(token);
	}
	
	/*
	 * This method calls the Box long polling APIs sequentially and asynchronously long polls the event stream
	 *   @param - Box Developer Token
	 */
	private static void callBoxAPI(String token) throws Exception	{
		BoxAPIFactory boxApi = new BoxAPIFactory();
		String current_stream_position = boxApi.getCurrentStreamPosition(token);
		String long_poll_url = boxApi.getLongPollURL(token);
		CompletableFuture<String> message = boxApi.startLongPoll(current_stream_position, long_poll_url, token);
		System.out.println(message.get());
		if(message != null && message.get().equals("reconnect"))
			callBoxAPI(token);
		else if( message != null && message.get().equals("new_change"))	{
			String eventDetails = boxApi.getEventDetails(current_stream_position, token);
			System.out.println(eventDetails);
			callBoxAPI(token);
		}
	}
}
