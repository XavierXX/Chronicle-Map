/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.map.impl;

import net.openhft.chronicle.hash.impl.stage.data.bytes.EntryKeyBytesData;
import net.openhft.chronicle.hash.impl.stage.entry.*;
import net.openhft.chronicle.hash.impl.stage.hash.KeyBytesInterop;
import net.openhft.chronicle.hash.impl.stage.hash.LogHolder;
import net.openhft.chronicle.hash.impl.stage.hash.OwnerThreadHolder;
import net.openhft.chronicle.hash.impl.stage.query.*;
import net.openhft.chronicle.map.impl.stage.data.DummyValueZeroData;
import net.openhft.chronicle.map.impl.stage.data.bytes.EntryValueBytesData;
import net.openhft.chronicle.map.impl.stage.data.instance.WrappedValueInstanceDataHolder;
import net.openhft.chronicle.map.impl.stage.map.*;
import net.openhft.chronicle.map.impl.stage.query.*;
import net.openhft.chronicle.map.impl.stage.ret.DefaultReturnValue;
import net.openhft.chronicle.map.impl.stage.ret.UsingReturnValue;
import net.openhft.sg.Context;
import net.openhft.sg.Staged;

@Staged
@Context(topLevel = {
        CompilationAnchor.class,
        OwnerThreadHolder.class,

        LogHolder.class,

        VanillaChronicleMapHolderImpl.class,

        KeyBytesInterop.class,
        QuerySegmentStages.class,
        KeySearch.class,
        InputKeyHashCode.class,
        QueryHashLookupSearch.class,
        HashLookupPos.class,

        QueryCheckOnEachPublicOperation.class,
        SearchAllocatedChunks.class,

        QueryMapEntryStages.class,
        MapEntryOperationsDelegation.class,
        WrappedValueInstanceValueHolder.class,
        MapQuery.class,
        MapAbsent.class,
        DefaultValue.class,
        ValueBytesInterop.class,
        MapAbsentHolder.class,

        QueryAlloc.class,
},
nested = {
        ReadLock.class,
        UpdateLock.class,
        WriteLock.class,

        EntryKeyBytesData.class,
        EntryValueBytesData.class,

        WrappedValueInstanceDataHolder.class,
        AcquireHandle.class,
        DefaultReturnValue.class,
        UsingReturnValue.class,

        HashKeyCrc32PayloadChecksumStrategy.class,

        DummyValueZeroData.class,
})
public class MapQueryContext {
}
