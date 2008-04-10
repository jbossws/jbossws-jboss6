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
package org.jboss.wsf.container.jboss50.invocation;

// $Id$

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.wsf.spi.invocation.integration.InvocationContextCallback;
import org.jboss.wsf.spi.invocation.integration.ServiceEndpointContainer;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.wsf.common.ObjectNameFactory;
import org.jboss.wsf.container.jboss50.ejb3.ServiceEndpointContainerApiAdapter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.util.KernelLocator;

import javax.management.ObjectName;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class InvocationHandlerEJB3 extends AbstractInvocationHandler
{
   private ObjectName objectName;
   private KernelController houston;

   InvocationHandlerEJB3()
   {
      houston = KernelLocator.getKernel().getController();
   }

   public Invocation createInvocation()
   {
      return new Invocation();
   }

   public void init(Endpoint ep)
   {
      String ejbName = ep.getShortName();
      ArchiveDeployment dep = (ArchiveDeployment)ep.getService().getDeployment();
      String nameStr = "jboss.j2ee:name=" + ejbName + ",service=EJB3,jar=" + dep.getSimpleName();
      if (dep.getParent() != null)
      {
         nameStr += ",ear=" + dep.getParent().getSimpleName();
      }

      objectName = ObjectNameFactory.create(nameStr.toString());

      if (houston.getInstalledContext( objectName.getCanonicalName() ) == null)
         throw new WebServiceException("Cannot find service endpoint target: " + objectName);
   }

   public void invoke(Endpoint ep, Invocation wsInv) throws Exception
   {
      try
      {         
         ControllerContext context = houston.getInstalledContext(objectName.getCanonicalName());
         ServiceEndpointContainer apiAdapter = ServiceEndpointContainerApiAdapter.createInstance(context.getTarget());

         Class beanClass = apiAdapter.getServiceImplementationClass();
         Method method = getImplMethod(beanClass, wsInv.getJavaMethod());
         Object[] args = wsInv.getArgs();
         InvocationContextCallback invProps = new EJB3InvocationContextCallback(wsInv);

         Object retObj = apiAdapter.invokeEndpoint(method, args, invProps);

         wsInv.setReturnValue(retObj);
      }
      catch (Throwable th)
      {
         handleInvocationException(th);
      }
   }

   static class EJB3InvocationContextCallback implements InvocationContextCallback
   {
      private Invocation wsInv;

      public EJB3InvocationContextCallback(Invocation wsInv)
      {
         this.wsInv = wsInv;
      }

      public <T> T get(Class<T> propertyType)
      {
         return wsInv.getInvocationContext().getAttachment(propertyType);               
      }
   }
}