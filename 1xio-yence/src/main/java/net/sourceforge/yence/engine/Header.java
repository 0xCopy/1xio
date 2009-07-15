/*
 * The Yence API. Details can be found at: http://yence.sourceforge.net
 * Copyright (C) 2002, Marcel Schepers
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307  USA
 */
package net.sourceforge.yence.engine;

/**
 *
 * <p><b>History:</b>
 * <ul>
 *   <li>20020424, Initial file</li>
 * </ul>
 *
 * <p><b>CVS Information:</b><br>
 * <i>
 * $Date: 2002/04/13 10:04:05 $<br>
 * $Revision: 1.29 $<br>
 * $Author: mgl $<br>
 * </i>
 *
 * @author  <a href="mailto:mgl@users.sourceforge.net">Marcel Schepers</a>
 */
public class Header {
  //
  // properties
  //
  private Integer line;
  private Integer size;
  private String name;
  
  /** 
   * Creates a new default instance of Header.
   */
  public Header() {
  }
  
  /** 
   * Creates a new instance of Header.
   *
   * @param headerLine the complete header as found in the encoded binary.
   */
  public Header(String headerLine) {
  }
  
  /** 
   * Creates a new instance of Header 
   *
   * @param line typical line length.
   * @param size size of the unencoded binary in bytes.
   * @param name name of unencoded binary file.
   */
  public Header(Integer line, Integer size, String name) {
    this.setLine(line);
    this.setSize(size);
    this.setName(name);
  }
  
  //
  // accessors
  //
  public Integer getLine(){
    return this.line;
  }
  
  public Integer getSize(){
    return this.size;
  }
  
  public String getName(){
    return this.name;
  }
  
  public void setLine(Integer line){
    this.line = line;
  }
  
  public void setSize(Integer size){
    this.size = size;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  //
  // helpers
  //
  public String toString(){
    return null;
  }
  
  //
  // helper classes
  //
  class HeaderParser{
  }
}
