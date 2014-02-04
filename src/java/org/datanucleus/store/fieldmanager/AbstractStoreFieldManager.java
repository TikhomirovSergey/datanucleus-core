/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
   ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;

/**
 * Abstract field manager for storage of objects.
 * To be extended by store plugins.
 */
public abstract class AbstractStoreFieldManager extends AbstractFieldManager
{
    protected ExecutionContext ec;

    protected ObjectProvider op;

    protected AbstractClassMetaData cmd;

    protected boolean insert;

    public AbstractStoreFieldManager(ObjectProvider op, boolean insert)
    {
        this.ec = op.getExecutionContext();
        this.op = op;
        this.cmd = op.getClassMetaData();
        this.insert = insert;
    }

    /**
     * Convenience method to return if the specified member is embedded.
     * @param mmd Metadata for the member we are interested in
     * @param relationType Relation type of the member we are interested in
     * @param ownerMmd Optional metadata for the owner member (for nested embeddeds only. Set to null if not relevant to the member in question).
     * @return Whether the member is embedded
     */
    protected boolean isMemberEmbedded(AbstractMemberMetaData mmd, RelationType relationType, AbstractMemberMetaData ownerMmd)
    {
        boolean embedded = false;
        if (relationType != RelationType.NONE)
        {
            // Determine if this relation field is embedded
            if (RelationType.isRelationSingleValued(relationType))
            {
                if (ownerMmd != null && ownerMmd.getEmbeddedMetaData() != null)
                {
                    // Is this a nested embedded (from JDO definition) with specification for this field?
                    AbstractMemberMetaData[] embMmds = ownerMmd.getEmbeddedMetaData().getMemberMetaData();
                    if (embMmds != null)
                    {
                        for (int i=0;i<embMmds.length;i++)
                        {
                            if (embMmds[i].getName().equals(mmd.getName()))
                            {
                                embedded = true;
                            }
                        }
                    }
                }
            }

            if (mmd.isEmbedded() || mmd.getEmbeddedMetaData() != null)
            {
                // Does this field have @Embedded definition?
                embedded = true;
            }
            else if (RelationType.isRelationMultiValued(relationType))
            {
                // Is this an embedded element/key/value?
                if (mmd.hasCollection() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
                {
                    // Embedded collection element
                    embedded = true;
                }
                else if (mmd.hasArray() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
                {
                    // Embedded array element
                    embedded = true;
                }
                else if (mmd.hasMap() && 
                        ((mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getEmbeddedMetaData() != null) || 
                        (mmd.getValueMetaData() != null && mmd.getValueMetaData().getEmbeddedMetaData() != null)))
                {
                    // Embedded map key/value
                    embedded = true;
                }
            }
        }

        return embedded;
    }

    protected boolean isStorable(int fieldNumber)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        return isStorable(mmd);
    }

    protected boolean isStorable(AbstractMemberMetaData mmd)
    {
        if (mmd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
        {
            // Member not persistent so ignore
            return false;
        }

        if ((insert && mmd.isInsertable()) || (!insert && mmd.isUpdateable()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}