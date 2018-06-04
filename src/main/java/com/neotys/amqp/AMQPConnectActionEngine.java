package com.neotys.amqp;

import static com.neotys.action.argument.Arguments.getArgumentLogString;
import static com.neotys.action.argument.Arguments.parseArguments;
import static com.neotys.amqp.AMQPConnectParameter.CONNECTIONNAME;
import static com.neotys.amqp.AMQPConnectParameter.HOSTNAME;
import static com.neotys.amqp.AMQPConnectParameter.PASSWORD;
import static com.neotys.amqp.AMQPConnectParameter.PORT;
import static com.neotys.amqp.AMQPConnectParameter.USERNAME;
import static com.neotys.amqp.AMQPConnectParameter.VIRTUALHOST;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.Logger;
import com.neotys.extensions.action.engine.SampleResult;
import com.neotys.jms.JmsResultFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public final class AMQPConnectActionEngine implements ActionEngine {

	private static final String STATUS_CODE_INVALID_PARAMETER = "NL-AMQP-CONNECT-ACTION-01";
	private static final String STATUS_CODE_ERROR_CONNECTION = "NL-AMQP-CONNECT-ACTION-02";

	@Override
	public SampleResult execute(final com.neotys.extensions.action.engine.Context context, final List<ActionParameter> parameters) {

		final Map<String, Optional<String>> parsedArgs;
		try {
			parsedArgs = parseArguments(parameters, AMQPConnectParameter.values());			
		} catch (final IllegalArgumentException iae) {
			return newErrorResult(context, "Executing AMQP Connect action.", STATUS_CODE_INVALID_PARAMETER, "Could not parse arguments: ", iae);
		}
		final String request = "Executing AMQP Connect action with parameters: " + getArgumentLogString(parsedArgs, AMQPConnectParameter.values()) + ".";
		final Logger logger = context.getLogger();
		if (logger.isDebugEnabled()) {
			logger.debug(request);
		}
		final String connectionName = parsedArgs.get(CONNECTIONNAME.getName()).or("");
		if (context.getCurrentVirtualUser().get(connectionName) != null) {
			return newErrorResult(context, request, STATUS_CODE_ERROR_CONNECTION, "A AMQP connection already exists with name " + connectionName + ".");
		}

		final ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost(parsedArgs.get(HOSTNAME.getName()).get());
		connectionFactory.setPort(Integer.parseInt(parsedArgs.get(PORT.getName()).get()));
		if (parsedArgs.get(USERNAME.getName()).isPresent()) {
			connectionFactory.setUsername(parsedArgs.get(USERNAME.getName()).get());
		}
		if (parsedArgs.get(PASSWORD.getName()).isPresent()) {
			connectionFactory.setPassword(parsedArgs.get(PASSWORD.getName()).get());
		}
		if (parsedArgs.get(VIRTUALHOST.getName()).isPresent()) {
			connectionFactory.setVirtualHost(parsedArgs.get(VIRTUALHOST.getName()).get());
		}

		Connection connection = null;
		try {
			connection = connectionFactory.newConnection();			
			return newOkResult(context, request, "Connected to AMQP server.");

		} catch (final Exception e) {
			return newErrorResult(context, request, STATUS_CODE_ERROR_CONNECTION, "Cannot create connection to AMQP server", e);
		} finally {
			context.getCurrentVirtualUser().put(connectionName, connection);			
		}
	}	

	private static SampleResult newErrorResult(final Context context, final String requestContent, final String statusCode, final String statusMessage, final Exception e) {
		final SampleResult result = JmsResultFactory.newErrorResult(context, statusCode, statusMessage, e);
		result.setRequestContent(requestContent);
		return result;
	}

	private static SampleResult newOkResult(final Context context, final String requestContent, final String statusMessage) {
		final SampleResult result = JmsResultFactory.newOkResult(context, statusMessage);
		result.setRequestContent(requestContent);
		return result;
	}


	private static SampleResult newErrorResult(final Context context, final String requestContent, final String statusCode, final String statusMessage) {
		final SampleResult result = JmsResultFactory.newErrorResult(context, statusCode, statusMessage);
		result.setRequestContent(requestContent);
		return result;
	}

	@Override
	public void stopExecute() {
		// Not implemented
	}

}