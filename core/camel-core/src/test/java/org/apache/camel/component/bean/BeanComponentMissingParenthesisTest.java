/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.bean;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BeanComponentMissingParenthesisTest extends ContextTestSupport {

    @Override
    protected Registry createRegistry() throws Exception {
        Registry jndi = super.createRegistry();
        jndi.bind("myBean", new MyContactBean());
        return jndi;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testCorrect() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("bean:myBean?method=concat(${body}, ${header.foo})").to("mock:result");
            }
        });
        context.start();

        getMockEndpoint("mock:result").expectedBodiesReceived("Hello=Camel");
        template.sendBodyAndHeader("direct:start", "Hello", "foo", "Camel");
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMissing() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("bean:myBean?method=concat(${body}, ${header.foo}").to("mock:result");
            }
        });
        context.start();

        CamelExecutionException e = assertThrows(CamelExecutionException.class,
                () -> template.sendBodyAndHeader("direct:start", "Hello", "foo", "Camel"),
                "Should throw exception");

        IllegalArgumentException iae = assertIsInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals("Method should end with parenthesis, was concat(${body}, ${header.foo}", iae.getMessage());
    }

    @Test
    public void testInvalidName() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("bean:myBean?method=--concat(${body}, ${header.foo})").to("mock:result");
            }
        });
        context.start();

        CamelExecutionException e = assertThrows(CamelExecutionException.class,
                () -> template.sendBodyAndHeader("direct:start", "Hello", "foo", "Camel"),
                "Should throw exception");

        assertIsInstanceOf(MethodNotFoundException.class, e.getCause());
    }

    public String doSomething(String body, String header) {
        return body + "=" + header;
    }

}
