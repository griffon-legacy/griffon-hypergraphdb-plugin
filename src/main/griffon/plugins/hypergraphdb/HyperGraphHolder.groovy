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

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import griffon.util.CallableWithArgs
import static griffon.util.GriffonNameUtils.isBlank

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
class HyperGraphHolder implements HypergraphdbProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HyperGraphHolder)
    private static final Object[] LOCK = new Object[0]
    private final Map<String, HyperGraph> databases = [:]
  
    String[] getHyperGraphNames() {
        List<String> databaseNames = new ArrayList().addAll(databases.keySet())
        databaseNames.toArray(new String[databaseNames.size()])
    }

    HyperGraph getHyperGraph(String databaseName = 'default') {
        if(isBlank(databaseName)) databaseName = 'default'
        retrieveHyperGraph(databaseName)
    }

    void setHyperGraph(String databaseName = 'default', HyperGraph database) {
        if(isBlank(databaseName)) databaseName = 'default'
        storeHyperGraph(databaseName, database)       
    }

    Object withHyperGraph(String databaseName = 'default', Closure closure) {
        HyperGraph database = fetchHyperGraph(databaseName)
        if(LOG.debugEnabled) LOG.debug("Executing statements on database '$databaseName'")
        return closure(databaseName, database)
    }

    public <T> T withHyperGraph(String databaseName = 'default', CallableWithArgs<T> callable) {
        HyperGraph database = fetchHyperGraph(databaseName)
        if(LOG.debugEnabled) LOG.debug("Executing statements on database '$databaseName'")
        callable.args = [databaseName, database] as Object[]
        return callable.call()
    }
    
    boolean isHyperGraphConnected(String databaseName) {
        if(isBlank(databaseName)) databaseName = 'default'
        retrieveHyperGraph(databaseName) != null
    }
    
    void disconnectHyperGraph(String databaseName) {
        if(isBlank(databaseName)) databaseName = 'default'
        storeHyperGraph(databaseName, null)        
    }

    private HyperGraph fetchHyperGraph(String databaseName) {
        if(isBlank(databaseName)) databaseName = 'default'
        HyperGraph database = retrieveHyperGraph(databaseName)
        if(database == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = HypergraphdbConnector.instance.createConfig(app)
            database = HypergraphdbConnector.instance.connect(app, config, databaseName)
        }
        
        if(database == null) {
            throw new IllegalArgumentException("No such Hypergraph configuration for name $databaseName")
        }
        database
    }

    private HyperGraph retrieveHyperGraph(String databaseName) {
        synchronized(LOCK) {
            databases[databaseName]
        }
    }

    private void storeHyperGraph(String databaseName, HyperGraph database) {
        synchronized(LOCK) {
            databases[databaseName] = database
        }
    }
}
