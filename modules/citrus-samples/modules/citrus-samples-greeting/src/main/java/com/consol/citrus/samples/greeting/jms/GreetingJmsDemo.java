/*
 * Copyright 2006-2010 ConSol* Software GmbH.
 * 
 * This file is part of Citrus.
 * 
 * Citrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Citrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Citrus. If not, see <http://www.gnu.org/licenses/>.
 */

package com.consol.citrus.samples.greeting.jms;

import com.consol.citrus.samples.CitrusSamplesDemo;

/**
 * @author Christoph Deppisch
 */
public class GreetingJmsDemo extends CitrusSamplesDemo {
    
    @Override
    protected String getDemoApplicationConfigLocation() {
        return "greeting-jms.xml";
    }
    
    @Override
    protected Class<? extends CitrusSamplesDemo> getDemoClass() {
        return GreetingJmsDemo.class;
    }
    
    /**
     * Main CLI method for running sample demo.
     * @param args, cli arguments
     */
    public static void main(String[] args) {
        GreetingJmsDemo demo = new GreetingJmsDemo();
        demo.start();
    }
}