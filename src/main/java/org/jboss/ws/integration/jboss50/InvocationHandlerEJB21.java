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
import java.security.Principal;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.ws.WebServiceException;

import org.jboss.ejb.EjbModule;
import org.jboss.ejb.Interceptor;
import org.jboss.ejb.StatelessSessionContainer;
import org.jboss.ejb.plugins.AbstractInterceptor;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.InvocationType;
import org.jboss.invocation.PayloadKey;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.security.SecurityAssociation;
import org.jboss.ws.integration.Endpoint;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.integration.invocation.HandlerCallback;
import org.jboss.ws.integration.invocation.InvocationContext;
import org.jboss.ws.integration.invocation.InvocationHandler;
import org.jboss.ws.metadata.j2ee.UnifiedApplicationMetaData;
import org.jboss.ws.metadata.j2ee.UnifiedBeanMetaData;
import org.jboss.ws.utils.ObjectNameFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB21 implements InvocationHandler
{
   // provide logging
   private static final Logger log = Logger.getLogger(InvocationHandlerEJB21.class);

   private String jndiName;
   private MBeanServer server;
   private ObjectName objectName;

   public void create(Endpoint endpoint)
   {
      server = MBeanServerLocator.locateJBoss();

      String ejbName = endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
      if (ejbName == null)
         throw new WebServiceException("Cannot obtain ejb-link from port component");

      UnifiedDeploymentInfo udi = endpoint.getService().getDeployment().getContext().getAttachment(UnifiedDeploymentInfo.class);
      UnifiedApplicationMetaData applMetaData = (UnifiedApplicationMetaData)udi.metaData;
      UnifiedBeanMetaData beanMetaData = (UnifiedBeanMetaData)applMetaData.getBeanByEjbName(ejbName);
      if (beanMetaData == null)
         throw new WebServiceException("Cannot obtain ejb meta data for: " + ejbName);

      // get the bean's JNDI name
      jndiName = beanMetaData.getContainerObjectNameJndiName();
      if (jndiName == null)
         throw new WebServiceException("Cannot obtain JNDI name for: " + ejbName);

      objectName = ObjectNameFactory.create("jboss.j2ee:jndiName=" + jndiName + ",service=EJB");

      // Dynamically add the service endpoint interceptor
      // http://jira.jboss.org/jira/browse/JBWS-758
      try
      {
         EjbModule ejbModule = (EjbModule)server.getAttribute(objectName, "EjbModule");
         StatelessSessionContainer container = (StatelessSessionContainer)ejbModule.getContainer(ejbName);

         boolean injectionPointFound = false;
         Interceptor prev = container.getInterceptor();
         while (prev != null && prev.getNext() != null)
         {
            Interceptor next = prev.getNext();
            if (next.getNext() == null)
            {
               log.debug("Inject service endpoint interceptor after: " + prev.getClass().getName());
               AbstractInterceptor sepInterceptor = endpoint.getAttachment(AbstractInterceptor.class);
               if (sepInterceptor == null)
                  throw new IllegalStateException("Cannot obtain endpoint interceptor");

               prev.setNext(sepInterceptor);
               sepInterceptor.setNext(next);
               injectionPointFound = true;
            }
            prev = next;
         }
         if (injectionPointFound == false)
            log.warn("Cannot service endpoint interceptor injection point");
      }
      catch (Exception ex)
      {
         log.warn("Cannot add service endpoint interceptor", ex);
      }

      if (server.isRegistered(objectName) == false)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);

   }

   public void start(Endpoint ep)
   {
   }

   public Object getTargetBean(Endpoint ep) throws InstantiationException, IllegalAccessException
   {
      return null;
   }

   public Object invoke(Endpoint ep, Object targetBean, Method method, Object[] args, InvocationContext context) throws Exception
   {
      // these are provided by the ServerLoginHandler
      Principal principal = SecurityAssociation.getPrincipal();
      Object credential = SecurityAssociation.getCredential();

      Invocation inv = new Invocation(null, method, args, null, principal, credential);

      //inv.setValue(InvocationKey.SOAP_MESSAGE_CONTEXT, msgContext);
      //inv.setValue(InvocationKey.SOAP_MESSAGE, msgContext.getSOAPMessage());
      inv.setType(InvocationType.SERVICE_ENDPOINT);

      HandlerCallback callback = ep.getAttachment(HandlerCallback.class);
      if (callback == null)
         throw new IllegalStateException("Cannot obtain handler callback");

      inv.setValue(HandlerCallback.class.getName(), callback, PayloadKey.TRANSIENT);
      inv.setValue(InvocationContext.class.getName(), context, PayloadKey.TRANSIENT);

      String[] sig = { Invocation.class.getName() };
      Object retObj = server.invoke(objectName, "invoke", new Object[] { inv }, sig);

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
}
