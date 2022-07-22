package pack;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.primitives.Primitives;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import pack.model.CustomPlacerRequest;
import pack.model.CustomPlacerResponse;
import pack.model.NetworkGraph;
import pack.placement.MyStack;
import pack.placement.SearchState;
import pack.placement.UCSRoutingAlgorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Main {

    public static WebSocketClient websocket;
    public static WebSocketSession websocketSession;
    private static ObjectMapper mapper;
    private static SearchState foundState = null;

    public static synchronized void send(WebSocketSession wss, WebSocketMessage<?> wsMsg) {
        try {
            wss.sendMessage(wsMsg);
        } catch (Exception e) {
        }
    }

    public static TextMessage createMessage(Object obj) {
        try {
            return (Primitives.isWrapperType(obj.getClass()) || (obj instanceof String)) ?
                    new TextMessage("" + obj) :
                    new TextMessage(mapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        Properties config = readConfig();

        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        websocket = new StandardWebSocketClient();
        System.out.println("Trying to connect to " + config.get("wsAddress") + "...");
        websocket.doHandshake(new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                websocketSession = session;
                System.out.println("Connection established!");
                send(session, createMessage("Placer"));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                String payloadString = null;
                byte payloadBytes[] = null;
                if (message instanceof TextMessage) {
                    payloadString = ((TextMessage) message).getPayload();
                } else {
                    payloadBytes = ((BinaryMessage) message).getPayload().array();
                }

                if ("Pong".equals(payloadString)) {
                    return;
                }

                if (payloadString != null) {
                    CustomPlacerRequest request = mapper.readValue(payloadString, CustomPlacerRequest.class);

                    System.out.println("Received placement request! (" + mapper.writeValueAsString(request.getParams()) + ")");
                    System.out.println("  Trying to place...");
                    foundState = null;
                    place(request);

                    CustomPlacerResponse response = new CustomPlacerResponse();
                    if (foundState == null) {
                        response.setSucceeded(false);
                        System.out.println("  Placement finished! Placer failed to place the service!");
                    } else {
                        response.setSucceeded(true);
                        response.setPlacedServiceNodes(foundState.getPlacedServiceNodes());
                        response.setPlacedNetworkNodes(foundState.getPlacedNetworkNodes());
                        response.setPlacedServiceLinks(foundState.getPlacedServiceLinks());
                        response.setPlacedPaths(foundState.getPlacedPaths());
                        System.out.println("  Placement finished! Placer could place the service successfully!");
                    }
                    send(session, createMessage(response));
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                System.out.println("Connection closed!");
            }
        }, "" + config.get("wsAddress"));

        while (true) {
            Thread.sleep(10000);
            send(websocketSession, createMessage("Ping"));
        }
    }

    public static void place(CustomPlacerRequest request) {
        MyStack stack = new MyStack();
        SearchState root = createRootState(request);
        stack.put(Arrays.asList(root));
        int counter = 0;
        while (!stack.isEmpty() && (counter < request.getParams().getTimeout()) && foundState == null) {
            expand(stack);
            counter++;
        }
    }

    private static SearchState createRootState(CustomPlacerRequest request) {
        NetworkGraph cloneNetwork = request.getNetwork().clone(false);
        SearchState root = new SearchState(cloneNetwork, request.getService().clone(), new UCSRoutingAlgorithm(cloneNetwork), 0);
        root.updateObjectiveValue();
        return root;
    }

    private static void expand(MyStack fringe) {
        SearchState state = fringe.take();
        if (state.isFeasible()) {
            if (state.isTerminal()) {
                foundState = state;
            } else {
                fringe.put(state.expand());
            }
        }
    }

    public static Properties readConfig() {
        Properties config = new Properties();
        try {
            FileInputStream is = new FileInputStream("config.txt");
            config.load(is);
        } catch (IOException e) {
            config.put("wsAddress", "ws://127.0.0.1:60100/ws");
        }
        return config;
    }

}
