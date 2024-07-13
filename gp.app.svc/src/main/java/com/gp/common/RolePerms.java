/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.common;

public class RolePerms {

	/**
	 * the user default case definition,
	 * Indicate the role is default role for certain cases
	 **/
	public static enum UserCase{
		NODE,   // from the same node server
		CENTER, // from the entity center server
		EXPATRIATOR, // from global server to work as employee
		COOPERATOR, // from global to work in a work group
		SYSTEM, // the system user
		MEMBER, // the normal groupress member alias for anonymous user
	}
}
