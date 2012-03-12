/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.hypergraphdb

import org.hypergraphdb.HyperGraph
import org.hypergraphdb.HGEnvironment
import org.hypergraphdb.HGConfiguration

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.CallableWithArgs

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class HypergraphdbConnector implements HypergraphdbProvider {
    private bootstrap

    private static final Logger LOG = LoggerFactory.getLogger(HypergraphdbConnector)

    Object withHyperGraph(String databaseName = 'default', Closure closure) {
        HyperGraphHolder.instance.withHyperGraph(databaseName, closure)
    }

    public <T> T withHyperGraph(String databaseName = 'default', CallableWithArgs<T> callable) {
        return HyperGraphHolder.instance.withHyperGraph(databaseName, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def databaseClass = app.class.classLoader.loadClass('HypergraphdbConfig')
        new ConfigSlurper(Environment.current.name).parse(databaseClass)
    }

    private ConfigObject narrowConfig(ConfigObject config, String databaseName) {
        return databaseName == 'default' ? config.database : config.databases[databaseName]
    }

    HyperGraph connect(GriffonApplication app, ConfigObject config, String databaseName = 'default') {
        if (HyperGraphHolder.instance.isHyperGraphConnected(databaseName)) {
            return HyperGraphHolder.instance.getHyperGraph(databaseName)
        }

        config = narrowConfig(config, databaseName)
        app.event('HypergraphdbConnectStart', [config, databaseName])
        HyperGraph database = startHyperGraph(config)
        HyperGraphHolder.instance.setHyperGraph(databaseName, database)
        bootstrap = app.class.classLoader.loadClass('BootstrapHypergraphdb').newInstance()
        bootstrap.metaClass.app = app
        bootstrap.init(databaseName, database)
        app.event('HypergraphdbConnectEnd', [databaseName, database])
        database
    }

    void disconnect(GriffonApplication app, ConfigObject config, String databaseName = 'default') {
        if (HyperGraphHolder.instance.isHyperGraphConnected(databaseName)) {
            config = narrowConfig(config, databaseName)
            HyperGraph database = HyperGraphHolder.instance.getHyperGraph(databaseName)
            app.event('HypergraphdbDisconnectStart', [config, databaseName, database])
            bootstrap.destroy(databaseName, database)
            stopHyperGraph(config, database)
            app.event('HypergraphdbDisconnectEnd', [config, databaseName])
            HyperGraphHolder.instance.disconnectHyperGraph(databaseName)
        }
    }

    private HyperGraph startHyperGraph(ConfigObject config) {
        String location  = config.location
        HGConfiguration hgConfig = new HGConfiguration()

        config.configuration?.each { key, value ->
            hgConfig[key] = value
        }

        HGEnvironment.get(location, hgConfig)
    }

    private void stopHyperGraph(ConfigObject config, HyperGraph database) {
        database.close()
    }
}
