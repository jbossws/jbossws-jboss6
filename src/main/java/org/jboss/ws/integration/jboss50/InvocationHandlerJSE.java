/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.integration.jboss50;

// $Id$

import java.lang.reflect.Method;

import javax.xml.ws.WebServiceContext;

import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.invocation.InvocationContext;
import org.jboss.ws.integration.invocation.InvocationHandler;
import org.jboss.ws.integration.invocation.WebServiceContextInjector;

/**
 * Handles invocations on JSE endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerJSE implements InvocationHandler
{
   public void create(Endpoint ep)
   {
      // Nothing to do
   }

   public void start(Endpoint endpoint)
   {
      // Nothing to do
   }

   public void stop(Endpoint endpoint)
   {
      // Nothing to do
   }

   public void destroy(Endpoint ep)
   {
      // Nothing to do
   }

   public Object getTargetBean(Endpoint endpoint) throws InstantiationException, IllegalAccessException
   {
      Class epImpl = endpoint.getTargetBean();
      Object targetBean = epImpl.newInstance();
      return targetBean;
   }

   public Object invoke(Endpoint endpoint, Object targetBean, Method method, Object[] args, InvocationContext context) throws Exception
   {
      if (context instanceof WebServiceContext)
         new WebServiceContextInjector().injectContext(targetBean, (WebServiceContext)context);

      Object retObj = method.invoke(targetBean, args);
      return retObj;
   }
}
