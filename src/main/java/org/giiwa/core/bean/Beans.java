/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.core.bean;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.json.JSON;

/**
 * The {@code Beans} Class used to contains the Bean in query. <br>
 * it's includes the total count for the query
 * 
 * @param <T>
 *          the generic type
 */
public final class Beans<E extends Bean> extends ArrayList<E> implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 2L;

  /** The log. */
  protected static Log      log              = LogFactory.getLog(Beans.class);

  /** The total. */
  public int                total            = -1;                            // unknown

  public JSON               stats;

  public long               cost             = -1;

  public List<String>       columns          = null;

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public JSON getStats() {
    return stats;
  }

  public void setStats(JSON stats) {
    this.stats = stats;
  }

  public long getCost() {
    return cost;
  }

  public void setCost(long cost) {
    this.cost = cost;
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Beans[total=" + total + ", size=" + size() + "]";
  }

}
