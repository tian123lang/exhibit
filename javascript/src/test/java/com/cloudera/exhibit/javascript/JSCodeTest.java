/*
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.exhibit.javascript;

import com.cloudera.exhibit.core.*;
import com.cloudera.exhibit.core.simple.SimpleExhibit;
import com.cloudera.exhibit.core.simple.SimpleFrame;
import com.cloudera.exhibit.core.simple.SimpleObs;
import com.cloudera.exhibit.core.simple.SimpleObsDescriptor;
import com.cloudera.exhibit.core.vector.VectorBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JSCodeTest {

  ObsDescriptor res1 = SimpleObsDescriptor.builder()
      .doubleField("a")
      .booleanField("b")
      .build();

  @Test
  public void testObs() throws Exception {
    JSCalculator jsc = new JSCalculator("var a = function() { return {a: df[0].a, b: true}; }; return a()");
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs obs = SimpleObs.of(od, 1729, true);
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Exhibit e = new SimpleExhibit(obs, ImmutableMap.of("df", frame));
    jsc.initialize(e.descriptor());
    long start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
      Obs res = Iterables.getOnlyElement(jsc.apply(e));
      assertEquals(SimpleObs.of(res1, 17.0, true), res);
    }
    System.out.println("Runtime: " + (System.currentTimeMillis() - start));
    jsc.cleanup();
  }

  @Test
  public void testVector() throws Exception {
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs obs = SimpleObs.of(od, 1729, true);
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Vec vector = VectorBuilder.doubles(ImmutableList.<Object>of(1.0, 2.0, 3.0));
    Exhibit e = new SimpleExhibit(obs, ImmutableMap.of("df", frame), ImmutableMap.of("v1", vector));

    ObsDescriptor resultObs = SimpleObsDescriptor.builder().doubleField("a").intField("b").build();
    JSCalculator jsc = new JSCalculator("var a = function() { return {a: v1[0], b: v1.length}; }; return a()");
    jsc.initialize(e.descriptor());
    Obs res = Iterables.getOnlyElement(jsc.apply(e));
    assertEquals(SimpleObs.of(resultObs, 1.0, 3), res);
    jsc.cleanup();
  }

  @Test
  public void testArray() throws Exception {
    JSCalculator jsc = new JSCalculator("var a = function() { return [{a: df[0].a, b: true}]; }; return a()");
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs obs = SimpleObs.of(od, 1729, true);
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Exhibit e = new SimpleExhibit(obs, ImmutableMap.of("df", frame));
    jsc.initialize(e.descriptor());
    Obs res = Iterables.getOnlyElement(jsc.apply(e));
    assertEquals(SimpleObs.of(res1, 17.0, true), res);
    jsc.cleanup();
  }

  @Test
  public void testLength() throws Exception {
    JSCalculator jsc = new JSCalculator("df.length");
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs obs = SimpleObs.of(od, 1729, true);
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Exhibit e = new SimpleExhibit(obs, ImmutableMap.of("df", frame));
    jsc.initialize(e.descriptor());
    Obs res = Iterables.getOnlyElement(jsc.apply(e));
    assertEquals(SimpleObs.of(SimpleObsDescriptor.of("res", FieldType.INTEGER), 2), res);
    jsc.cleanup();
  }

  @Test
  public void testFrame() throws Exception {
    JSCalculator jsc = new JSCalculator("df");
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs obs = SimpleObs.of(od, 1729, true);
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Exhibit e = new SimpleExhibit(obs, ImmutableMap.of("df", frame));
    jsc.initialize(e.descriptor());
    assertEquals(frame, jsc.apply(e));
    jsc.cleanup();
  }

  @Test
  public void testFunctor() throws Exception {
    JSFunctor jsf = new JSFunctor("var a = function() { return [{a: df[0].a, b: true}]; }; f = a(); b = ['', 'a']; c = 1;");
    ObsDescriptor od = SimpleObsDescriptor.builder().doubleField("a").booleanField("b").build();
    Obs one = SimpleObs.of(od, 17, true);
    Obs two = SimpleObs.of(od, 12, false);
    Frame frame = SimpleFrame.of(one, two);
    Exhibit e = new SimpleExhibit(Obs.EMPTY, ImmutableMap.of("df", frame));
    jsf.initialize(e.descriptor());
    Exhibit res = jsf.apply(e);
    assertEquals(1.0, (Double) res.attributes().get("c"), 0.001);
    assertEquals("a", res.vectors().get("b").get(1));
    assertEquals(Boolean.TRUE, res.frames().get("f").get(0).get(1));
    jsf.cleanup();
  }
}
