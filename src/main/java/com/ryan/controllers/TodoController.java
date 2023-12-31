package com.ryan.controllers;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.ryan.data.TodoDatabaseRepository;
import com.ryan.data.UserDatabaseRepository;
import com.ryan.models.Result;
import com.ryan.models.Todo;
import com.ryan.service.TodoService;
import com.ryan.util.ConnectionFactory;
import com.ryan.util.JWTBuilder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public class TodoController {

	public static void getTodos(HttpServletRequest req, HttpServletResponse res) {
		Connection conn = ConnectionFactory.getConnection();
		TodoService todoService = new TodoService(new TodoDatabaseRepository(conn), new UserDatabaseRepository(conn));
		try {
			ObjectMapper om = new ObjectMapper();
			Jws<Claims> jws = JWTBuilder.parseJWS(req.getHeader("Authorization"));
			if (jws == null) {
				res.setStatus(401);
			} else {
				int userId = (int) jws.getBody().get("userId");
				res.setStatus(200);
				res.getWriter().write(om.writeValueAsString(todoService.getTodosByUser(userId)));
			}
		} catch (IOException e) {
			res.setStatus(400);
		}
	}
	
	public static void addTodo(HttpServletRequest req, HttpServletResponse res) {
		Connection conn = ConnectionFactory.getConnection();
		TodoService todoService = new TodoService(new TodoDatabaseRepository(conn), new UserDatabaseRepository(conn));
		try {
			ObjectMapper om = new ObjectMapper();
			JsonNode jsonNode = om.readTree(req.getReader());
			JsonNode userIdNode = jsonNode.get("userId");
			JsonNode taskNode = jsonNode.get("task");
			JsonNode completedNode = jsonNode.get("completed");
			Jws<Claims> jws = JWTBuilder.parseJWS(req.getHeader("Authorization"));
			if (jws == null) {
				res.setStatus(401);
			} else {
				Todo todo = new Todo();
				if (userIdNode != null && !(userIdNode instanceof NullNode)) {
					todo.setUserId(userIdNode.asInt());
				}
				if (taskNode != null && !(taskNode instanceof NullNode)) {
					todo.setTask(taskNode.asText());
				}
				if (completedNode != null && !(completedNode instanceof NullNode)) {
					todo.setCompleted(completedNode.asBoolean());
				}
				int userId = (int) jws.getBody().get("userId");
				if (userId != todo.getUserId()) {
					res.setStatus(403);
				} else {
					Result<Todo> result = todoService.createTodo(todo);
					if (result.isSuccess()) {
						res.setStatus(200);
					} else {
						res.setStatus(400);
					}
					res.getWriter().write(om.writeValueAsString(result));
				}
			}
		} catch (IOException e) {
			res.setStatus(400);
		}
	}
	
	public static void updateTodo(HttpServletRequest req, HttpServletResponse res) {
		Connection conn = ConnectionFactory.getConnection();
		TodoService todoService = new TodoService(new TodoDatabaseRepository(conn), new UserDatabaseRepository(conn));
		try {
			ObjectMapper om = new ObjectMapper();
			JsonNode jsonNode = om.readTree(req.getReader());
			JsonNode todoIdNode = jsonNode.get("todoId");
			JsonNode userIdNode = jsonNode.get("userId");
			JsonNode taskNode = jsonNode.get("task");
			JsonNode completedNode = jsonNode.get("completed");
			Jws<Claims> jws = JWTBuilder.parseJWS(req.getHeader("Authorization"));
			if (jws == null) {
				res.setStatus(401);
			} else {
				Todo todo = new Todo();
				if (todoIdNode != null && !(todoIdNode instanceof NullNode)) {
					todo.setTodoId(todoIdNode.asInt());
				}
				if (userIdNode != null && !(userIdNode instanceof NullNode)) {
					todo.setUserId(userIdNode.asInt());
				}
				if (taskNode != null && !(taskNode instanceof NullNode)) {
					todo.setTask(taskNode.asText());
				}
				if (completedNode != null && !(completedNode instanceof NullNode)) {
					todo.setCompleted(completedNode.asBoolean());
				}
				int userId = (int) jws.getBody().get("userId");
				Result<Todo> existingTodoResult = todoService.getTodoById(todo.getTodoId());
				if (existingTodoResult.isSuccess() && userId != existingTodoResult.getPayload().getUserId()) {
					res.setStatus(403);
				} else {
					Result<Todo> result = todoService.updateTodo(todo);
					if (result.isSuccess()) {
						res.setStatus(200);
					} else {
						res.setStatus(400);
					}
					res.getWriter().write(om.writeValueAsString(result));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			res.setStatus(400);
		}
	}
	
	public static void deleteTodo(HttpServletRequest req, HttpServletResponse res) {
		Connection conn = ConnectionFactory.getConnection();
		TodoService todoService = new TodoService(new TodoDatabaseRepository(conn), new UserDatabaseRepository(conn));
		String[] params = req.getRequestURI().split("/");
		try {
			int todoId = Integer.parseInt(params[3]);
			Result<Todo> result = todoService.getTodoById(todoId);
			ObjectMapper om = new ObjectMapper();
			Jws<Claims> jws = JWTBuilder.parseJWS(req.getHeader("Authorization"));
			if (jws == null) {
				res.setStatus(401);
			} else {
				int userId = (int) jws.getBody().get("userId");
				if (!result.isSuccess()) {
					res.setStatus(400);
					res.getWriter().write(om.writeValueAsString(result));
				} else {
					if (userId != result.getPayload().getUserId()) {
						res.setStatus(403);
					} else {
						Result<Todo> deleteResult = todoService.deleteTodo(todoId);
						if (deleteResult.isSuccess()) {
							res.setStatus(200);
						} else {
							res.setStatus(400);
						}
						res.getWriter().write(om.writeValueAsString(deleteResult));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			res.setStatus(400);
		}
	}
}
