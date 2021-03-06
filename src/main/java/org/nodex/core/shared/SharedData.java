/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.nodex.core.shared;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.concurrent.ConcurrentMap;

public class SharedData {

  private static ConcurrentMap<Object, SharedMap<?, ?>> maps = new NonBlockingHashMap<>();
  private static ConcurrentMap<Object, SharedSet<?>> sets = new NonBlockingHashMap<>();

  public static <K, V> SharedMap<K, V> getMap(Object name) {
    SharedMap<K, V> map = (SharedMap<K, V>)maps.get(name);
    if (map == null) {
      map = new SharedMap<>();
      SharedMap prev = maps.putIfAbsent(name, map);
      if (prev != null) {
        map = prev;
      }
    }
    return map;
  }

  public static <E> SharedSet<E> getSet(Object name) {
    SharedSet<E> set = (SharedSet<E>)sets.get(name);
    if (set == null) {
      set = new SharedSet<>();
      SharedSet prev = sets.putIfAbsent(name, set);
      if (prev != null) {
        set = prev;
      }
    }
    return set;
  }

  public static boolean removeMap(Object name) {
    return maps.remove(name) != null;
  }

  public static boolean removeSet(Object name) {
    return sets.remove(name) != null;
  }
}
