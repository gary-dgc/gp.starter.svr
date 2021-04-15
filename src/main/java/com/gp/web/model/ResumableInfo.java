/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.model;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.gp.info.AccessPoint;

/**
 * ResumableInfo wrap the necessary data for chunks uploading.
 * 
 * @author gary diao
 */
public class ResumableInfo {

    public int      chunkSize;
    public long     totalSize;
    public String   identifier;
    public String   fileName;

    @JsonIgnore
    public AccessPoint accessPoint;

    //Chunks uploaded
    @JsonIgnore
    public Set<ChunkNumber> chunks = new HashSet<ChunkNumber>();

    public String filePath;

    /**
     * Get the chunks count
     **/
    public int getChunkCount(){
        return (int) Math.ceil(((double) totalSize) / ((double) chunkSize));
    }
    
    /**
     * Check if all chunks finished 
     **/
    public boolean checkFinished() {
        //check if upload finished
        int count = (int) Math.ceil(((double) totalSize) / ((double) chunkSize));
        for(int i = 1; i < count; i ++) {
        	
            if (!chunks.contains(new ChunkNumber(i))) {
                return false;
            }
        }

        //Upload finished, change filename.
        File file = new File(filePath);
        String new_path = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - ".temp".length());
        file.renameTo(new File(new_path));
        this.filePath = new_path;
        
        return true;
    }
    
    /**
     * Get specified chunk number 
     **/
    @JsonIgnore
    public ChunkNumber getChunkNumber(int number){
    	for(ChunkNumber chunk : chunks) {
    		if(number == chunk.number) return chunk;
    	}
    	
    	return null;
    }
    
    /**
     * Class to wrap the chunk number 
     **/
    public static class ChunkNumber {
    	
        public int chunkSize;
        
        public int number;

        public ChunkNumber(){}
        
        public ChunkNumber(int number){
        	this.number = number;
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj instanceof ChunkNumber
                    ? ((ChunkNumber)obj).number == this.number : false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(number);
        }
    }
}


