/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.webservices.integration.invocation;

import java.lang.reflect.Method;

import javax.xml.ws.WebServiceException;

import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.common.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.integration.InvocationContextCallback;
import org.jboss.wsf.spi.invocation.integration.ServiceEndpointContainer;
import org.jboss.wsf.spi.ioc.IoCContainerProxy;
import org.jboss.wsf.spi.ioc.IoCContainerProxyFactory;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class InvocationHandlerEJB3 extends AbstractInvocationHandler
{

   /** MC kernel controller. */
   private final IoCContainerProxy iocContainer;

   /** EJB3 container name. */
   private String containerName;

   /** EJB3 container. */
   private ServiceEndpointContainer serviceEndpointContainer;

   /**
    * Constructor.
    */
   InvocationHandlerEJB3()
   {
      super();

      final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      final IoCContainerProxyFactory iocContainerFactory = spiProvider.getSPI(IoCContainerProxyFactory.class);
      this.iocContainer = iocContainerFactory.getContainer();
   }

   /**
    * Initializes EJB3 container name.
    * 
    * @param endpoint web service endpoint
    */
   public void init(final Endpoint endpoint)
   {
      this.containerName = (String) endpoint.getProperty(ASHelper.CONTAINER_NAME);

      if (this.containerName == null)
      {
         throw new IllegalArgumentException("Container name cannot be null");
      }
   }

   /**
    * Gets EJB 3 container lazily.
    * 
    * @return EJB3 container
    */
   private synchronized ServiceEndpointContainer getEjb3Container()
   {
      final boolean ejb3ContainerNotInitialized = this.serviceEndpointContainer == null;

      if (ejb3ContainerNotInitialized)
      {
         this.serviceEndpointContainer = this.iocContainer.getBean(this.containerName, ServiceEndpointContainer.class);
         if (this.serviceEndpointContainer == null)
         {
            throw new WebServiceException("Cannot find service endpoint target: " + this.containerName);
         }
      }

      return this.serviceEndpointContainer;
   }

   /**
    * Invokes EJB 3 endpoint.
    * 
    * @param endpoint EJB 3 endpoint
    * @param wsInvocation web service invocation
    * @throws Exception if any error occurs
    */
   public void invoke(final Endpoint endpoint, final Invocation wsInvocation) throws Exception
   {
      try
      {
         // prepare for invocation
         final ServiceEndpointContainer ejbContainer = this.getEjb3Container();
         final InvocationContextCallback invocationCallback = new EJB3InvocationContextCallback(wsInvocation);
         final Class<?> implClass = ejbContainer.getServiceImplementationClass();
         final Method seiMethod = wsInvocation.getJavaMethod();
         final Method implMethod = this.getImplMethod(implClass, seiMethod);
         final Object[] args = wsInvocation.getArgs();

         // invoke method
         final Object retObj = ejbContainer.invokeEndpoint(implMethod, args, invocationCallback);
         wsInvocation.setReturnValue(retObj);
      }
      catch (Throwable t)
      {
         this.log.error("Method invocation failed with exception: " + t.getMessage(), t);
         this.handleInvocationException(t);
      }
   }

   /**
    * EJB3 invocation callback allowing EJB 3 beans to access Web Service invocation properties.
    */
   private static final class EJB3InvocationContextCallback implements InvocationContextCallback
   {

      /** WebService invocation. */
      private Invocation wsInvocation;

      /**
       * Constructor.
       * 
       * @param wsInvocation delegee
       */
      public EJB3InvocationContextCallback(final Invocation wsInvocation)
      {
         this.wsInvocation = wsInvocation;
      }

      /**
       * Retrieves attachment type from Web Service invocation context attachments.
       * 
       * @param <T> attachment type
       * @param attachmentType attachment class
       * @return attachment value
       */
      public <T> T get(final Class<T> attachmentType)
      {
         return this.wsInvocation.getInvocationContext().getAttachment(attachmentType);
      }

   }

}
