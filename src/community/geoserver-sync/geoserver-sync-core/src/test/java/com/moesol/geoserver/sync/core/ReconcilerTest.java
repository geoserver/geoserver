/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.core;




import static org.mockito.Mockito.*;

import org.mockito.InOrder;

import com.moesol.geoserver.sync.core.Reconciler;
import com.moesol.geoserver.sync.grouper.ReconcileTestBase;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

public class ReconcilerTest extends ReconcileTestBase {
	
	Sha1SyncJson local = makeJson().level(0);
	Sha1SyncJson remote = makeJson().level(0);
	
	// TODO expand to include local/remote
	interface ReconcileNotices {
		public void differentAtPosition(String position);
		public void matchAtPosition(String position);
		public void localMissingPosition(String position);
		public void remoteMissingPosition(String position);
	}
	class ReconcilerNotifier extends Reconciler {
		private final ReconcileNotices m_notices;

		public ReconcilerNotifier(Sha1SyncJson local, Sha1SyncJson remote, ReconcileNotices notices) {
			super(local, remote);
			m_notices = notices;
		}

		@Override
		protected void differentAtPosition(String position, Sha1SyncPositionHash local, Sha1SyncPositionHash remote) {
			m_notices.differentAtPosition(position);
		}

		@Override
		protected void matchAtPosition(String position) {
			m_notices.matchAtPosition(position);
		}

		@Override
		protected void localMissingPosition(String position, Sha1SyncPositionHash missing) {
			m_notices.localMissingPosition(position);
		}

		@Override
		protected void remoteMissingPosition(String position, Sha1SyncPositionHash missing) {
			m_notices.remoteMissingPosition(position);
		}
		
	}
	
	public void testMsg() {
		Sha1SyncJson remote = makeJson().level(1);
		try {
			new ReconcilerNotifier(makeJson(), remote, null);
			fail("ex");
		} catch (IllegalArgumentException e) {
			assertEquals("Local level(0) not equal to remote level(1)", e.getMessage());
		}
	}
	
	public void testEmptyRemote() {
		local.h.add(makePosition("aa", "0001"));
		local.h.add(makePosition("bb", "1111"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).remoteMissingPosition("aa");
		inOrder.verify(notices).remoteMissingPosition("bb");
		verifyNoMoreInteractions(notices);
	}

	public void testEmptyLocal() {
		remote.h.add(makePosition("aa", "0001"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).localMissingPosition("aa");
		verifyNoMoreInteractions(notices);
	}
	
	public void testRemoteMissingOneAfter() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("aa", "0000"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).remoteMissingPosition("bb");
		verifyNoMoreInteractions(notices);
	}

	public void testRemoteMissingOneBefore() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("bb", "1111"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).remoteMissingPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		verifyNoMoreInteractions(notices);
	}
	
	public void testLocalMissingOneAfter() {
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("aa", "0000"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).localMissingPosition("bb");
		verifyNoMoreInteractions(notices);
	}

	public void testLocalMissingOneBefore() {
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("bb", "1111"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).localMissingPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		verifyNoMoreInteractions(notices);
	}
	
	public void testMatch() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("cc", "2222"));
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("cc", "2222"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		inOrder.verify(notices).matchAtPosition("cc");
		verifyNoMoreInteractions(notices);
	}
	
	public void testDiffInLocalBefore() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("cc", "2222"));
		remote.h.add(makePosition("aa", "00dd"));
		remote.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("cc", "2222"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).differentAtPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		inOrder.verify(notices).matchAtPosition("cc");
		verifyNoMoreInteractions(notices);
	}
	public void testDiffInLocalMiddle() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("cc", "2222"));
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "11dd"));
		remote.h.add(makePosition("cc", "2222"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).differentAtPosition("bb");
		inOrder.verify(notices).matchAtPosition("cc");
		verifyNoMoreInteractions(notices);
	}
	public void testDiffInLocalAfter() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("cc", "2222"));
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("cc", "22dd"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		inOrder.verify(notices).differentAtPosition("cc");
		verifyNoMoreInteractions(notices);
	}
	
	public void testMixOfDiffs() {
		local.h.add(makePosition("aa", "0000"));
		local.h.add(makePosition("bb", "1111"));
		local.h.add(makePosition("cc", "2222"));
		local.h.add(makePosition("dd", "3333"));
		remote.h.add(makePosition("aa", "0000"));
		remote.h.add(makePosition("bb", "1111"));
		remote.h.add(makePosition("cc", "22dd"));
		remote.h.add(makePosition("ee", "4444"));
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aa");
		inOrder.verify(notices).matchAtPosition("bb");
		inOrder.verify(notices).differentAtPosition("cc");
		inOrder.verify(notices).remoteMissingPosition("dd");
		inOrder.verify(notices).localMissingPosition("ee");
		verifyNoMoreInteractions(notices);
	}
	
	public void testRemoteMissingHigherTree() {
		local.h.add(makePosition("aaaa", "0000"));
		local.h.add(makePosition("aabb", "1111"));
		local.h.add(makePosition("bbaa", "2222"));
		local.h.add(makePosition("bbbb", "3333"));
		remote.h.add(makePosition("aaaa", "0000"));
		remote.h.add(makePosition("aabb", "1111"));
		remote.h.add(makePosition("bb", null)); // remote side is telling us to delete all bb's
		ReconcileNotices notices = mock(ReconcileNotices.class);
		ReconcilerNotifier reconciler = new ReconcilerNotifier(local, remote, notices);
		reconciler.reconcile();
		
		InOrder inOrder = inOrder(notices);
		inOrder.verify(notices).matchAtPosition("aaaa");
		inOrder.verify(notices).matchAtPosition("aabb");
		inOrder.verify(notices).localMissingPosition("bb");
		inOrder.verify(notices).remoteMissingPosition("bbaa");
		inOrder.verify(notices).remoteMissingPosition("bbbb");
		verifyNoMoreInteractions(notices);
	}
	
}
