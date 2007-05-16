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

import javax.ejb.EJBContext;
import javax.management.ObjectName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;

import org.jboss.aop.Dispatcher;
import org.jboss.aop.MethodInfo;
import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.BeanContextLifecycleCallback;
import org.jboss.ejb3.EJBContainerInvocation;
import org.jboss.ejb3.stateless.StatelessBeanContext;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.injection.lang.reflect.BeanProperty;
import org.jboss.logging.Logger;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.invocation.InvocationContext;
import org.jboss.ws.integration.invocation.InvocationHandler;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB3 implements InvocationHandler
{
   // provide logging
   private static final Logger log = Logger.getLogger(InvocationHandlerEJB3.class);

   private ObjectName objectName;

   public void create(Endpoint endpoint)
   {
      String ejbName = endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
      if (ejbName == null)
         throw new WebServiceException("Cannot obtain ejb-link");

      UnifiedDeploymentInfo udi = endpoint.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      String nameStr = "jboss.j2ee:name=" + ejbName + ",service=EJB3,jar=" + udi.simpleName;
      if (udi.parent != null)
      {
         nameStr += ",ear=" + udi.parent.simpleName;
      }

      objectName = ObjectNameFactory.create(nameStr.toString());
   }

   public void start(Endpoint ep)
   {
      Dispatcher dispatcher = Dispatcher.singleton;
      if (dispatcher.getRegistered(objectName.getCanonicalName()) == null)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);
   }

   public Object getTargetBean(Endpoint ep) throws InstantiationException, IllegalAccessException
   {
      return null;
   }

   public Object invoke(Endpoint ep, Object targetBean, Method method, Object[] args, InvocationContext context) throws Exception
   {
      Dispatcher dispatcher = Dispatcher.singleton;
      StatelessContainer container = (StatelessContainer)dispatcher.getRegistered(objectName.getCanonicalName());

      MethodInfo info = container.getMethodInfo(method);

      EJBContainerInvocation<StatelessContainer, StatelessBeanContext> ejb3Inv = new EJBContainerInvocation<StatelessContainer, StatelessBeanContext>(info);
      ejb3Inv.setAdvisor(container);
      ejb3Inv.setArguments(args);
      ejb3Inv.setContextCallback(new ContextCallback(context));

      Object retObj;
      try
      {
         retObj = ejb3Inv.invokeNext();
      }
      catch (Exception ex)
      {
         throw ex;
      }
      catch (Throwable ex)
      {
         throw new RuntimeException(ex);
      }
      return retObj;
   }

   public void stop(Endpoint ep)
   {
      // Nothing to do
   }

   public void destroy(Endpoint ep)
   {
      // Nothing to do
   }

   class ContextCallback implements BeanContextLifecycleCallback
   {
      private WebServiceContext wsContext;

      public ContextCallback(InvocationContext context)
      {
         if (context instanceof WebServiceContext)
            this.wsContext = (WebServiceContext)context;
      }

      public void attached(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;

         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null && wsContext instanceof WebServiceContext)
         {
            EJBContext ejbCtx = beanCtx.getEJBContext();
            beanProp.set(beanCtx.getInstance(), wsContext);
         }
      }

      public void released(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;

         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null)
            beanProp.set(beanCtx.getInstance(), null);
      }
   }
}
