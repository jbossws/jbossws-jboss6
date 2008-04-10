/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.wsf.container.jboss50.ejb3;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.BeanContextLifecycleCallback;
import org.jboss.ejb3.stateless.StatelessBeanContext;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.injection.lang.reflect.BeanProperty;
import org.jboss.wsf.spi.invocation.integration.InvocationContextCallback;
import org.jboss.wsf.spi.invocation.integration.ServiceEndpointContainer;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.invocation.ExtensibleWebServiceContext;
import org.jboss.wsf.spi.invocation.InvocationType;
import org.jboss.wsf.spi.invocation.WebServiceContextFactory;

import javax.ejb.EJBContext;
import java.lang.reflect.Method;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class ServiceEndpointContainerApiAdapter implements ServiceEndpointContainer
{
   private StatelessContainer ejb3Container;

   public static ServiceEndpointContainer createInstance(Object invocationTarget)
   {
      if(! (invocationTarget instanceof StatelessContainer) )
         throw new IllegalArgumentException("Unexpected invocation target: " + invocationTarget);

      return new ServiceEndpointContainerApiAdapter((StatelessContainer)invocationTarget);
   }

   ServiceEndpointContainerApiAdapter(StatelessContainer ejb3Container)
   {
      this.ejb3Container = ejb3Container;
   }


   public Class getServiceImplementationClass()
   {
      return ejb3Container.getBeanClass();
   }

   public Object invokeEndpoint(Method method, Object[] args, InvocationContextCallback invocationContextCallback)
     throws Throwable   
   {
      CallbackImpl callback = new CallbackImpl(invocationContextCallback);
      return ejb3Container.localInvoke(method, args, null, callback);
   }

   static class CallbackImpl implements BeanContextLifecycleCallback
   {
      private javax.xml.ws.handler.MessageContext jaxwsMessageContext;
      private javax.xml.rpc.handler.MessageContext jaxrpcMessageContext;      

      public CallbackImpl(InvocationContextCallback epInv)
      {
         jaxrpcMessageContext = epInv.get( javax.xml.rpc.handler.MessageContext.class );
         jaxwsMessageContext = epInv.get( javax.xml.ws.handler.MessageContext.class );
      }

      public void attached(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;
         sbc.setMessageContextJAXRPC(jaxrpcMessageContext);

         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null)
         {
            EJBContext ejbCtx = beanCtx.getEJBContext();
            SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
            ExtensibleWebServiceContext wsContext = spiProvider.getSPI(WebServiceContextFactory.class).newWebServiceContext(InvocationType.JAXWS_EJB3, jaxwsMessageContext);
            wsContext.addAttachment(EJBContext.class, ejbCtx);
            beanProp.set(beanCtx.getInstance(), wsContext);
         }
      }

      public void released(BeanContext beanCtx)
      {
         StatelessBeanContext sbc = (StatelessBeanContext)beanCtx;
         sbc.setMessageContextJAXRPC(null);

         BeanProperty beanProp = sbc.getWebServiceContextProperty();
         if (beanProp != null)
            beanProp.set(beanCtx.getInstance(), null);
      }
   }
}
