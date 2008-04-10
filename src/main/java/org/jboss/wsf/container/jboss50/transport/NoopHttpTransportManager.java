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
package org.jboss.wsf.container.jboss50.transport;

import org.jboss.wsf.framework.transport.HttpListenerRef;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;
import org.jboss.wsf.spi.transport.HttpSpec;
import org.jboss.wsf.spi.transport.ListenerRef;
import org.jboss.wsf.spi.transport.TransportManager;
import org.jboss.wsf.spi.transport.TransportSpec;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * TransportManager implementation that doesn't create physical
 * endpoints (ports, etc) but simply constructs a service endpoint address for a given
 * deployment;
 *  
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class NoopHttpTransportManager implements TransportManager
{
   public ListenerRef createListener(Endpoint endpoint, TransportSpec transportSpec)
   {
      if(! (transportSpec instanceof HttpSpec))
         throw new IllegalArgumentException("Unknown TransportSpec " + transportSpec);

      HttpSpec httpSpec = (HttpSpec)transportSpec;

      SPIProvider provider = SPIProviderResolver.getInstance().getProvider();
      ServerConfigFactory spi = provider.getSPI(ServerConfigFactory.class);
      ServerConfig serverConfig = spi.getServerConfig();

      String host = serverConfig.getWebServiceHost();
      int port = serverConfig.getWebServicePort();
      String hostAndPort = host + (port > 0 ? ":" + port : "");
      
      try
      {
         String ctx = httpSpec.getWebContext();
         String pattern = httpSpec.getUrlPattern();
         ListenerRef ref =  new HttpListenerRef(
           ctx, pattern,
           new URI("http://"+hostAndPort+ctx+pattern)
         );

         return ref;

      } catch (URISyntaxException e)
      {
         throw new RuntimeException("Failed to create ListenerRef", e);
      }
   }

   public void destroyListener(ListenerRef ref)
   {
      // noop  
   }
}
