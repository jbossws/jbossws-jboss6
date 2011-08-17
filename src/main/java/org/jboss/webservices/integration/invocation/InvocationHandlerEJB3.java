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
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;

import org.jboss.ejb3.EJBContainer;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.ws.api.util.BundleUtils;
import org.jboss.ws.common.injection.ThreadLocalAwareWebServiceContext;
import org.jboss.ws.common.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;
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
   private static final ResourceBundle bundle = BundleUtils.getBundle(InvocationHandlerEJB3.class);
   /** EJB3 JNDI context. */
   private static final String EJB3_JNDI_PREFIX = "java:env/";

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
         throw new IllegalArgumentException(BundleUtils.getMessage(bundle, "CONTAINER_NAME_CANNOT_BE_NULL"));
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
            throw new WebServiceException(BundleUtils.getMessage(bundle, "CANNOT_FIND_SERVICE_ENDPOINT_TARGET",  this.containerName));
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
         this.onBeforeInvocation(wsInvocation);
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
         this.log.error(BundleUtils.getMessage(bundle, "METHOD_INVOCATION_FAILED",  t.getMessage()),  t);
         this.handleInvocationException(t);
      }
      finally
      {
         this.onAfterInvocation(wsInvocation);
      }
   }

   public Context getJNDIContext(final Endpoint ep) throws NamingException
   {
      final EJBContainer ejb3Container = (EJBContainer) getEjb3Container();
      return (Context) ejb3Container.getEnc().lookup(EJB3_JNDI_PREFIX);
   }

   /**
    * Injects webservice context on target bean.
    *
    *  @param invocation current invocation
    */
   @Override
   public void onBeforeInvocation(final Invocation invocation)
   {
      final WebServiceContext wsContext = this.getWebServiceContext(invocation);
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(wsContext);
   }

   /**
    * Cleanups injected webservice context on target bean.
    *
    * @param invocation current invocation
    */
   @Override
   public void onAfterInvocation(final Invocation invocation)
   {
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(null);
   }

   /**
    * Returns WebServiceContext associated with this invocation.
    *
    * @param invocation current invocation
    * @return web service context or null if not available
    */
   private WebServiceContext getWebServiceContext(final Invocation invocation)
   {
      final InvocationContext invocationContext = invocation.getInvocationContext();

      return invocationContext.getAttachment(WebServiceContext.class);
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
