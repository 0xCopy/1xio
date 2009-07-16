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
public class Trailer {
  //
  // properties
  //
  private Integer size;
  
  /** 
   * Creates a new default instance of Header.
   */
  public Trailer() {
  }
  
  /** 
   * Creates a new instance of Header.
   *
   * @param headerLine the complete header as found in the encoded binary.
   */
  public Trailer(String trailerLine) {
  }
  
  /** 
   * Creates a new instance of Header 
   *
   * @param size size of the unencoded binary in bytes.
   */
  public Trailer(Integer size) {
    this.setSize(size);
  }
  
  //
  // accessors
  //
  public Integer getSize(){
    return this.size;
  }
  
  public void setSize(Integer size){
    this.size = size;
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
  class TrailerParser{
  }
}
