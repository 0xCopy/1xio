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
package net.sourceforge.yence;

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
public interface YEnc {
  
  /**
   * Version of Yence engine.
   */
  public static final String YENCE_VERSION = "0.00.00";
  
  /**
   * Version of yEncode supported by Yence.
   */
  public static final String YENCODE_VERSION = "1.2";
  
  /**
   * Marks the beginning of a yEnc keyword line.
   *
   * All keyword lines must begin with an escape character ('='), followed by an
   * ASCII 79h ('y').  This '=y' combination uniquely identifies a line as a
   * keyword line, since 'y' is not a valid encoded critical character. <br/>
   *
   * <p><font size="-1">
   * <i>source: <a href="http://www.easynews.com/yenc/yenc-draft.1.3.txt">
   * yEncode - A quick and dirty encoding for binaries version 1.2 </a></i>
   * </font></p>
   */
  public static final String KEYWORD_LINE_MARKER = "=y";
  
  /**
   * Marks the beginning of yEnc data block.
   *
   * Header lines must always begin with the "ybegin" keyword, and contain the
   * typical line length, the size of the original unencoded binary (in bytes),
   * and the name of the original binary file. 
   *
   * The filename must always be the last item on the header line.  This ensures
   * that all characters and character sequences may be included in the filename
   * without interfering with other keywords.  Although quotes (ASCII 22h, '"')
   * are technically permitted, they are not recommended for use in filenames.
   *e
   * Note: the trailing space.
   *
   * <p><font size="-1">
   * <i>source: <a href="http://www.easynews.com/yenc/yenc-draft.1.3.txt">
   * yEncode - A quick and dirty encoding for binaries version 1.2 </a></i>
   * </font></p>
   */
  public static final String BEGIN_BLOCK_MARKER = "=ybegin ";
  
  /**
   * Marks the end of yEnc data block.
   *
   * Header lines must always begin with the "ybegin" keyword, and contain the
   * typical line length, the size of the original unencoded binary (in bytes),
   * and the name of the original binary file.
   *
   * Note: the trailing space.
   *
   * <p><font size="-1">
   * <i>source: <a href="http://www.easynews.com/yenc/yenc-draft.1.3.txt">
   * yEncode - A quick and dirty encoding for binaries version 1.2 </a></i>
   * </font></p>
   */
  public static final String END_BLOCK_MARKER = "=yend ";
}