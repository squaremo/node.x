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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class SharedMap<K, V> implements ConcurrentMap<K, V> {

  private final ConcurrentMap<K, V> map = new NonBlockingHashMap<>();

  public V putIfAbsent(K k, V v) {
    k = SharedUtils.checkObject(k);
    v = SharedUtils.checkObject(v);
    return map.putIfAbsent(k, v);
  }

  public boolean remove(Object o, Object o1) {
    return map.remove(o, o1);
  }

  public boolean replace(K k, V v, V v1) {
    k = SharedUtils.checkObject(k);
    v1 = SharedUtils.checkObject(v1);
    return map.replace(k, v, v1);
  }

  public V replace(K k, V v) {
    k = SharedUtils.checkObject(k);
    v = SharedUtils.checkObject(v);
    return map.replace(k, v);
  }

  public int size() {
    return map.size();
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public boolean containsKey(Object o) {
    return map.containsKey(o);
  }

  public boolean containsValue(Object o) {
    return map.containsValue(o);
  }

  public V get(Object o) {
    return map.get(o);
  }

  public V put(K k, V v) {
    k = SharedUtils.checkObject(k);
    v = SharedUtils.checkObject(v);
    return map.put(k, v);
  }

  public V remove(Object o) {
    return map.remove(o);
  }

  public void putAll(Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> entry: map.entrySet()) {
      K k = SharedUtils.checkObject(entry.getKey());
      V v = SharedUtils.checkObject(entry.getValue());
      this.map.put(k, v);
    }
  }

  public void clear() {
    map.clear();
  }

  public Set<K> keySet() {
    return map.keySet();
  }

  public Collection<V> values() {
    return map.values();
  }

  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return map.equals(o);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }
}
