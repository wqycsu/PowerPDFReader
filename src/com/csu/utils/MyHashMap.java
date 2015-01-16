package com.csu.utils;

import java.util.HashMap;

public class MyHashMap<String,V> extends HashMap<String,V> implements Comparable<MyHashMap<String,V>>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int compareTo(MyHashMap<String,V> o) {
		// TODO Auto-generated method stub
		return (this.keySet().toArray()[0].toString().compareTo((java.lang.String)(o.keySet().toArray()[0])));
	}
	
}
