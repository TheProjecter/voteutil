package org.bolson.vote;

import java.util.HashMap;

/**
Virtual Round Robin election (Condorcet).
 @see <a href="http://en.wikipedia.org/wiki/Condorcet's_Method">Condorcet's Method (Wikipedia)</a>
 @author Brian Olson
 */
public class VRR extends NameVotingSystem implements SummableVotingSystem, IndexVotable {
	/** "DUMMY_CHOICE_NAME" is standin for choices not voted on yet so that write-ins count correctly. */
	protected static final String dummyname = "DUMMY_CHOICE_NAME";
	/** HashMap<String,Count> maps choice names to Count.
	 @see Count */
	protected HashMap counts = new HashMap();
	/** Always call getIndexedCounts(). */
	protected Count[] countIndexCache = null;
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;
	/** intermediate count calculated during getWinners */
	protected int defeatCount[] = null;
	/** Set by htmlExplain
	 @see #htmlExplain(StringBuffer)
	 */
	protected StringBuffer explain = null;
	
	/**
	Enable Ranked Pairs Mode by passing "rp" to init(String[])
	@see #init(String[])
	*/	
	protected boolean rankedPairsMode = false;

	/**
	 Checks arguments to modify this VRR.
	 "winningVotes" and "margins" modify cycle resolution.
	 @see #winningVotes
	 @see #margins
	 */
	public int init( String argv[] ) {
		if ( argv == null ) {
			return 0;
		}
		for ( int i = 0; i < argv.length; ++i ) {
			if ( argv[i] == null ) {
				// skip
			} else if ( argv[i].equals("winningVotes") ) {
				winningVotes = true;
				margins = false;
				argv[i] = null;
			} else if ( argv[i].equals("margins") ) {
				winningVotes = false;
				margins = true;
				argv[i] = null;
			} else if ( argv[i].equals("rp") ) {
				rankedPairsMode = true;
			}
		}
		return super.init( argv );
	}
	
	public VRR() {
		counts.put( dummyname, new Count( dummyname, 0 ) );
	}
	
	/**
	 Record a vote.
	 Keeps only a summation of the votes, not individual vote data.
	*/
	public void voteRating( NameVote[] vote ) {
		// See also parallel implementation in voteIndexVoteSet()
		Count[] cs = new Count[vote.length];
		// minimize hash table lookup, and force fork if any new names. need to fork before modifying any data.
		for ( int i = 0; i < vote.length; i++ ) {
			cs[i] = getCount( vote[i].name );
		}
		// clear cached solution
		winners = null;
		// Apply votes for choices voted on
		for ( int i = 0; i < vote.length; i++ ) {
			Count a;
			a = cs[i];
			for ( int j = i + 1; j < vote.length; j++ ) {
				Count b;
				b = cs[j];
				if ( a.index == b.index ) {
					// ignore duplicate names on vote
					continue;
				}
				try {
				if ( vote[i].rating > vote[j].rating ) {
					if ( a.index > b.index ) {
						a.counts[b.index]++; // a wins
					} else {
						b.counts[b.index + a.index]++; // b looses
					}
				} else if ( vote[j].rating > vote[i].rating ) {
					if ( a.index > b.index ) {
						a.counts[a.index + b.index]++; // a looses
					} else {
						b.counts[a.index]++; // b wins
					}
				}
				} catch ( ArrayIndexOutOfBoundsException ae ) {
					System.out.println("<p>ERROR: vote["+i+"].name="+vote[i].name+", rating="+vote[i].rating+", index="+a.index+"<br>vote["+j+"].name="+vote[j].name+", rating="+vote[j].rating+", index="+b.index+"</p>");
					Thread.dumpStack();
				}
			}
		}
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		for ( java.util.Iterator ci = counts.entrySet().iterator(); ci.hasNext(); ) {
			java.util.Map.Entry e = (java.util.Map.Entry)ci.next();
			String name = (String)e.getKey();
			Count d = (Count)e.getValue();
			boolean isvoted = false;
			for ( int i = 0; i < vote.length; i++ ) {
				if ( d == cs[i] || name.equals( vote[i].name ) ) {
					isvoted = true;
					break;
				}
			}
			if ( isvoted ) {
				continue;
			}
			// name wasn't voted on.
			for ( int i = 0; i < vote.length; i++ ) {
				Count a;
				a = cs[i];
				if ( vote[i].rating >= 0 ) {
					if ( a.index > d.index ) {
						a.counts[d.index]++; // a wins
					} else {
						d.counts[d.index + a.index]++; // d looses
					}
				} else if ( 0 > vote[i].rating ) {
					if ( a.index > d.index ) {
						a.counts[a.index + d.index]++; // a looses
					} else {
						d.counts[a.index]++; // d wins
					}
				}
			}
		}
	}
	public void voteIndexVoteSet(IndexVoteSet vote) {
		// See also parallel implementation in voteRating()
		Count[] cs = new Count[vote.index.length];
		// minimize hash table lookup, and force fork if any new names. need to fork before modifying any data.
		for ( int i = 0; i < vote.index.length; i++ ) {
			cs[i] = getCount( Integer.toString(vote.index[i] + 1) );
		}
		// clear cached solution
		winners = null;
		// Apply votes for choices voted on
		for ( int i = 0; i < vote.rating.length; i++ ) {
			Count a;
			a = cs[i];
			for ( int j = i + 1; j < vote.rating.length; j++ ) {
				Count b;
				b = cs[j];
				if ( a.index == b.index ) {
					// ignore duplicate names on vote
					continue;
				}
				try {
				if ( vote.rating[i] > vote.rating[j] ) {
					if ( a.index > b.index ) {
						a.counts[b.index]++; // a wins
					} else {
						b.counts[b.index + a.index]++; // b looses
					}
				} else if ( vote.rating[j] > vote.rating[i] ) {
					if ( a.index > b.index ) {
						a.counts[a.index + b.index]++; // a looses
					} else {
						b.counts[a.index]++; // b wins
					}
				}
				} catch ( ArrayIndexOutOfBoundsException ae ) {
					System.out.println("<p>ERROR: vote["+i+"].name="+vote.index[i]+", rating="+vote.rating[i]+", index="+a.index+"<br>vote["+j+"].name="+vote.index[j]+", rating="+vote.rating[j]+", index="+b.index+"</p>");
					Thread.dumpStack();
				}
			}
		}
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		for ( java.util.Iterator ci = counts.entrySet().iterator(); ci.hasNext(); ) {
			java.util.Map.Entry e = (java.util.Map.Entry)ci.next();
			String name = (String)e.getKey();
			Count d = (Count)e.getValue();
			boolean isvoted = false;
			for ( int i = 0; i < cs.length; i++ ) {
				if ( d == cs[i] || name.equals( cs[i].name ) ) {
					isvoted = true;
					break;
				}
			}
			if ( isvoted ) {
				continue;
			}
			// name wasn't voted on.
			for ( int i = 0; i < vote.rating.length; i++ ) {
				Count a;
				a = cs[i];
				if ( vote.rating[i] >= 0 ) {
					if ( a.index > d.index ) {
						a.counts[d.index]++; // a wins
					} else {
						d.counts[d.index + a.index]++; // d looses
					}
				} else if ( 0 > vote.rating[i] ) {
					if ( a.index > d.index ) {
						a.counts[a.index + d.index]++; // a looses
					} else {
						d.counts[a.index]++; // d wins
					}
				}
			}
		}
	}

	/** Vote a partial tally into this voting system.
	 @param other another SummableVotingSystem. Most likely it will only work if it is the same class as this.
	 @throws ClassCastExepcion if other isn't the same or compatible with this
	 */
	public void accumulateSubVote( SummableVotingSystem other ) throws ClassCastException {
		if ( other == null ) return;
		if ( ! (other instanceof VRR) ) {
			throw new ClassCastException("don't know how to add "+other.getClass().getName()+" into "+this.getClass().getName() );
		}
		VRR it = (VRR)other;
		Count[] othercounts = it.getIndexedCounts(true);
		// Apply to this like a big vote
		Count[] cs = new Count[othercounts.length];
		// minimize hash table lookup, and force fork if any new names. need to fork before modifying any data.
		// This also serves to map other-VRR-index to this-VRR-index
		for ( int i = 0; i < othercounts.length; i++ ) {
			cs[i] = getCount( othercounts[i].name );
		}
		if ( debug && (debugLog != null) ) {
			htmlDebugTable( debugLog, othercounts, "other counts" );
			htmlDebugTable( debugLog, cs, "this counts" );
		}
		// clear cached solution
		winners = null;
		// Apply votes for choices voted on in the other VRR
		for ( int i = 0; i < othercounts.length; i++ ) {
			Count oci = othercounts[i];
			Count thisi = cs[i];
			for ( int j = 0; j < oci.index; j++ ) {
				Count thisj = cs[j];
				if ( thisi.index > thisj.index ) {
					thisi.counts[thisj.index] += oci.winsVs(j);
					thisi.counts[thisi.index + thisj.index] += oci.lossesVs(j);
				} else if ( thisj.index > thisi.index ) {
					thisj.counts[thisi.index] += oci.lossesVs(j);
					thisj.counts[thisj.index + thisi.index] += oci.winsVs(j);
				} else {
					System.err.println("ERROR"); Thread.dumpStack();
				}
			}
		}
		/*
		 FIXME WRITEME, this isn't right.
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		// Act as if these names were being added as copies of the dummy vote in that, and add those votes to each side of that pairing in this.
		for ( java.util.Iterator ci = counts.entrySet().iterator(); ci.hasNext(); ) {
			java.util.Map.Entry e = (java.util.Map.Entry)ci.next();
			String name = (String)e.getKey();
			Count d = (Count)e.getValue();
			boolean isvoted = false;
			for ( int i = 0; i < othercounts.length; i++ ) {
				if ( d == cs[i] || name.equals( othercounts[i].name ) ) {
					isvoted = true;
					break;
				}
			}
			if ( isvoted ) {
				continue;
			}
			// A name in this wasn't voted on in that, use dummy votes from that to apply non-votes to name in this.
			for ( int i = 0; i < othercounts.length; i++ ) {
				Count a;
				a = cs[i];
				if ( vote[i].rating >= 0 ) {
					if ( a.index > d.index ) {
						a.counts[d.index]++; // a wins
					} else {
						d.counts[d.index + a.index]++; // d looses
					}
				} else if ( 0 > vote[i].rating ) {
					if ( a.index > d.index ) {
						a.counts[a.index + d.index]++; // a looses
					} else {
						d.counts[a.index]++; // d wins
					}
				}
			}
		}
		 */
		if ( debug && (debugLog != null) ) {
			htmlDebugTable( debugLog, cs, "this counts" );
		}
	}

	protected static NameVote[] makeWinners( Count[] they, int[] defeatCount ) {
		NameVote[] winners = new NameVote[they.length];
		int i;
		for ( i = 0; i < they.length; i++ ) {
			winners[i] = new NameVote( they[i].name, (float)(defeatCount[i] * -1.0) );
		}
		sort( winners );
		return winners;
	}
	/**
		find Condorcet winner with CSSD cycle resolution
	 */
	public NameVote[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		Count[] they = getIndexedCounts( false );
		if ( explain != null ) {
			if ( explainVerbosity >= 2 ) {
				htmlDebugTable( explain, getIndexedCounts( true ), null );
			} else if ( explainVerbosity >= 1 ) {
				htmlDebugTable( explain, they, null );
			}
		}
		int numc = they.length;
		if ( numc == 0 ) {
			return new NameVote[0];
		}
		int[] tally = getTallyArray( they );
		defeatCount = new int[numc];

		countDefeats( numc, tally, defeatCount );
		for ( int i = 0; i < numc; i++ ) {
			if ( defeatCount[i] == 0 ) {
				// we have a winner
				winners = makeWinners( they, defeatCount );
				return winners;
			}
		}
		if ( rankedPairsMode ) {
			return getWinnersRankedPairs( they, tally );
		} else {
			return getWinnersCSSD( they, tally );
		}
	}
	
	/** Extract counts in order of name index from counts mapping. 
	 @see #counts
	 */
	protected Count[] getIndexedCounts( boolean ldebug ) {
		if ( (countIndexCache != null) && (!ldebug) ) {
			// only use cache for non-dummy-inclusive version
			return countIndexCache;
		}
		int numi = counts.size();
		if ( ! ldebug ) {
			numi--;
		}
		Count[] they = new Count[numi];
		java.util.Iterator ti = counts.values().iterator();
		int i = 0, j;
		while ( ti.hasNext() ) {
			Count tc;
			tc = (Count)ti.next();
			if ( (! ldebug) && tc.name.equals( dummyname ) ) {
				// skip
			} else {
				they[i] = tc;
				i++;
			}
		}
		// sort on index
		for ( i = 0; i < they.length; i++ ) {
			for ( j = i + 1; j < they.length; j++ ) {
				if ( they[i].index > they[j].index ) {
					Count tc = they[i];
					they[i] = they[j];
					they[j] = tc;
				}
			}
		}
		if ( !ldebug ) {
			// only use cache for non-dummy-inclusive version
			countIndexCache = they;
		}
		return they;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		if ( getWinners() == null ) {
			return sb;
		}
		if ( debugLog != null ) {
			sb.append( "<pre>debug:\n" );
			sb.append( debugLog );
			sb.append( "</pre>" );
		}
		Count[] they = getIndexedCounts( debug );
		sb = htmlTable( sb, winners, they, false );
		if (false) {
		// this is less useful now based on changes to the pair table.
		sb.append( "<table border=\"1\">" );
		for ( int i = 0; i < winners.length; i++ ) {
			sb.append( "<tr><th>" );
			sb.append( i+1 );
			sb.append( "</th><td>" );
			sb.append( winners[i].name );
			sb.append( "</tr>" );
		}
		sb.append( "</table>" );
		}
		return sb;
	}
	public StringBuffer htmlExplain( StringBuffer sb ) {
		explain = sb;
		sb = htmlSummary( sb );
		sb = htmlBeatByBlock( sb );
		explain = null;
		return sb;
	}
	public String name() {
		if (rankedPairsMode) {
			return "Virtual Round Robin, Ranked Pairs Resolution";
		} else {
			return "Virtual Round Robin";
		}
	}

	/**
	 Holds a portion of the VRR table.
	 Holds beats and defeats for this.name vs all others voted on before it.
	 */
	protected static class Count {
		String name;
		int index;
/**
counts laid out in concentric rings.
x, y and z indicate higher indexed counts.
A 0 x y z
B x 0 y z
C y y 0 z
D z z z 0
*/
		public int[] counts;
		Count( String nin, int ix ) {
			name = nin;
			index = ix;
			counts = new int[index * 2];
		}
		
		/**
		 @param otherIndex must be less than this.index
		 */
		int winsVs(int otherIndex) {
			return counts[otherIndex];
		}
		/**
		 @param otherIndex must be less than this.index
		 */
		int lossesVs(int otherIndex) {
			return counts[index + otherIndex];
		}
	};
	protected Count getCount( String name ) {
		Count c = (Count)counts.get( name );
		if ( c == null ) {
			countIndexCache = null;
			Count d;
			d = (Count)counts.get( dummyname );
			c = new Count( name, counts.size() );
			for ( int j = 0; j < d.index; j++ ) {
				c.counts[j] = d.counts[j];
				c.counts[j + c.index] = d.counts[j + d.index];
			}
			// swap index and counts so dummy is always outtermost
			int ti = c.index;
			c.index = d.index;
			d.index = ti;
			int[] tc = c.counts;
			c.counts = d.counts;
			d.counts = tc;
			counts.put( c.name, c );
		}
		return c;
	}
	
	/** build a tally array like Condorcet class uses, easier to use that code with */
	protected static int[] getTallyArray( Count[] they ) {
		int numc = they.length;
		int[] tally = new int[numc*numc];
		for ( int i = 0; i < they.length; i++ ) {
			for ( int j = 0; j < they.length; j++ ) {
				if ( i == j ) {
					tally[i*numc + j] = 0;
				} else if ( i > j ) {
					tally[i*numc + j] = they[i].counts[j];
				} else /*if ( i < j )*/ {
					tally[i*numc + j] = they[j].counts[j + i];
				}
			}
		}
		return tally;
	}
	
	/** Break cycles based on which defeat A>B in the cycle has the fewest votes for A>B.
	 One of winningVotes and margins should be true.
	 @see #margins
	 */
	public boolean winningVotes = true;
	/** Break cycles based on which defeat A>B in the cycle has smallest difference between votes for A>B and votes for B>A.
	 One of winningVotes and margins should be true.
	 @see #winningVotes
	 */
	public boolean margins = false;

	static class minij {
		int ihi;
		int ilo;
	}
	
	/** Cycle resolution.
	 I believe this correctly implements Cloneproof Schwartz Set Dropping, aka the Schulze method.
	 http://wiki.electorama.com/wiki/Schulze_method
	 */
	public NameVote[] getWinnersCSSD( Count[] they, int[] tally ) {
		// cloneproof schwartz set dropping
		// which ought to be the same as above "beatpath" method, but a new implementation
		int numc = they.length;
		int[] ss = getSchwartzSet( numc, tally, defeatCount, debugLog );
		int mind; // minimum defeat, index and strength
		minij[] mins = new minij[numc];
		mins[0] = new minij();
		
		boolean notdone = true;
		int tie = 0;
		int round = 0;
		
		while ( notdone ) {
			round++;
			notdone = false;
			mind = Integer.MAX_VALUE;
			tie = 0;
			if ( explain != null ) {
				//assert(ss.length > 0);
				explain.append("<p>Top choices: ");
				explain.append(they[ss[0]].name);
				for ( int i= 1; i < ss.length; ++i ) {
					explain.append(", ");
					explain.append(they[ss[i]].name);
				}
				explain.append("</p>\n");
			}
			if ( debugLog != null ) {
				debugLog.append("schwartz set = { ");
				debugLog.append( they[ss[0]].name );
				for ( int j = 1; j < ss.length; j++ ) {
					debugLog.append(", ");
					debugLog.append( they[ss[j]].name );
				}
				debugLog.append(" }\ndefeats: ").append(they[0].name).append("=").append(defeatCount[0]);
				for ( int j = 1; j < defeatCount.length; j++ ) {
					debugLog.append(", ").append(they[j].name).append("=").append(defeatCount[j]);
				}
				debugLog.append("\n");
			}
			// find weakest defeat between members of schwartz set
			for ( int ji = 0; ji < ss.length - 1; ji++ ) {
				int j;
				j = ss[ji];
				for ( int ki = ji + 1; ki < ss.length; ki++ ) {
					int k;
					int vj, vk;
					int ihi, ilo;
					int vhi, vlo;
					int m;

					k = ss[ki];
					vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
					vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
					if ((vk == -1) && (vj == -1)) {
						continue;
					}
					if ( vk > vj ) {
						ihi = k;
						ilo = j;
						vhi = vk;
						vlo = vj;
					} else {
						ihi = j;
						ilo = k;
						vhi = vj;
						vlo = vk;
					}
					if ( winningVotes ) {
						m = vhi;
					} else if ( margins ) {
						m = vhi - vlo;
					} else {
						m = 100000000;
						//assert(false);
					}
					if ( m < mind ) {
						tie = 1;
						mind = m;
						mins[0].ihi = ihi;
						mins[0].ilo = ilo;
					} else if ( m == mind ) {
						if ( mins[tie] == null ) {
							mins[tie] = new minij();
						}
						mins[tie].ihi = ihi;
						mins[tie].ilo = ilo;
						tie++;
					}
				}
			}
			if ( tie == 0 ) {
				if ( debugLog != null ) {
					debugLog.append("tie = 0, no weakest defeat found to cancel\n");
				}
				return winners = makeWinners( they, defeatCount );
			}
			// all are tied
			if ( tie == ss.length) {
				if ( debugLog != null ) {
					debugLog.append("tie==ss.length, mind=").append(mind).append('\n');
				}
				return winners = makeWinners( they, defeatCount );
			}
			for ( int i = 0; i < tie; ++i ) {
				int mindk = mins[i].ihi;
				int mindj = mins[i].ilo;
				tally[mindk*numc + mindj] = -1;
				tally[mindj*numc + mindk] = -1;
				defeatCount[mindj]--;
				if ( debugLog != null ) {
					debugLog.append( "set " ).append( they[mindk].name ).append( " vs " ).append( they[mindj].name ).append(" = -1\n");
					htmlDebugTable( debugLog, numc, tally, "intermediate", they );
				}
				if ( explain != null ) {
					explain.append("<p>");
					explain.append(round);
					explain.append(": Weakest defeat is ");
					explain.append(they[mindk].name);
					explain.append(" (");
					explain.append(tally[mindk*numc + mindj]);
					explain.append(") v ");
					explain.append(they[mindj].name);
					explain.append(" (");
					explain.append(tally[mindj*numc + mindk]);
					explain.append("). ");
					explain.append(they[mindj].name);
					explain.append(" has one fewer defeat.</p>\n");
				}
			}
			ss = getSchwartzSet( numc, tally, defeatCount, debugLog );
			if ( ss.length == 1 ) {
				return winners = makeWinners( they, defeatCount );
			}
			notdone = true;
		}
		return winners = makeWinners( they, defeatCount );
	}

	/**
	Temporary data for Ranked Pairs cycle resolution.
	*/
	protected static class Pair implements Comparable {
		public int winner;
		public int loser;
		public int Vwl;
		public int Vlw;
		public boolean active;
		
		public Pair(int winner_in, int loser_in, int win_v_loss, int loss_v_win) {
			winner = winner_in;
			loser = loser_in;
			Vwl = win_v_loss;
			Vlw = loss_v_win;
			active = true;
		}

		public int compareTo(Object o) throws ClassCastException {
			if (o instanceof Pair) {
				Pair b = (Pair)o;
				if (Vwl > b.Vwl) {
					return -1;
				}
				if (Vwl < b.Vwl) {
					return 1;
				}
				if (Vlw < b.Vlw) {
					return -1;
				}
				if (Vlw > b.Vlw) {
					return 1;
				}
				return 0;
			}
			throw new ClassCastException("not a Pair");
		}
		
		public boolean equals(Object o) throws ClassCastException {
			if (o instanceof Pair) {
				Pair b = (Pair)o;
				return (winner == b.winner) && (loser == b.loser) &&
					(Vwl == b.Vwl) && (Vlw == b.Vlw) && (active == b.active);
			}
			throw new ClassCastException("not a Pair");
		}
	}

	/**
	Ranked Pairs condorcet cycle resolution.
	@see <a href="http://wiki.electorama.com/wiki/Ranked_Pairs">Ranked Pairs article on Electorama wiki</a>
	*/
	public NameVote[] getWinnersRankedPairs( Count[] they, int[] tally ) {
		int numc = they.length;
		java.util.ArrayList rankset = new java.util.ArrayList();
		int x = 0;
		for ( int i = 0; i < numc; ++i ) {
			if ( debugLog != null ) {
				debugLog.append(i).append(": ").append(they[i].name).append("\n");
			}
			for ( int j = i + 1; j < numc; ++j ) {
				int ij, ji;
				ij = tally[i*numc + j];	// i beat j ij times
				ji = tally[j*numc + i];	// j beat i ji times
				if ( ij > ji ) {
					if ( debugLog != null ) { debugLog.append(i).append(" > ").append(j).append("\n"); }
					rankset.add(new Pair(i, j, ij, ji));
					x++;
				} else if ( ji > ij ) {
					if ( debugLog != null ) { debugLog.append(j).append(" > ").append(i).append("\n"); }
					rankset.add(new Pair(j, i, ji, ij));
					x++;
				} else {
					// tie policy?
					if ( explain != null ) {
						explain.append("<p>not adding a ranked pair for (\"").append(they[i].name).append("\" vs \"").append(they[j].name).append("\") due to tie</p>\n");
					}
				}
			}
		}
		if ( debugLog != null ) { debugLog.append("x=").append(x).append(" rankset.size()=").append(rankset.size()).append("\n"); }
		Pair[] ranks = new Pair[rankset.size()];
		java.util.Iterator t = rankset.iterator();
		x = 0;
		while (t.hasNext()) {
			ranks[x] = (Pair)t.next();
			x++;
		}
		java.util.Arrays.sort(ranks);
		//assert(x == ranks.length);
		if ( explain != null ) {
			explain.append( "<p>Initial pair rankings:</p><table border=\"0\">\n" );
			for ( int i = 0; i < ranks.length; ++i ) {
				explain.append("<tr><td>");
				explain.append(they[ranks[i].winner].name);
				explain.append("</td><td>&gt;</td><td>");
				explain.append(they[ranks[i].loser].name);
				explain.append("</td><td>(");
				explain.append(ranks[i].Vwl);
				explain.append(" &gt; ");
				explain.append(ranks[i].Vlw);
				explain.append(")</td></tr>\n");
			}
			explain.append("</table>\n<p>");
		}
		if ( debugLog != null ) {
			for ( int i = 0; i < ranks.length; ++i ) {
				debugLog.append("ranks[").append(i).append("] ").append(ranks[i].winner).append(" > ").append(ranks[i].loser).append("\n");
			}
		}
		int i = 1;
		while ( i < x ) {
			int equivalenceLimit = i;
			while ( (equivalenceLimit + 1) < x ) {
				if (ranks[equivalenceLimit].compareTo(ranks[equivalenceLimit + 1]) != 0) {
					break;
				}
				++equivalenceLimit;
			}
			// (equivalenceLimit + 1) is the first pair not tied with the pair at i
			if ( findPath(ranks, equivalenceLimit, ranks[i].loser, ranks[i].winner, null) ) {
				// drop this link as there is a pre-existing reverse path
				if ( explain != null ) {
					explain.append("DROP: ");
					explain.append(they[ranks[i].winner].name);
					explain.append(" &gt; ");
					explain.append(they[ranks[i].loser].name);
					explain.append(" (");
					explain.append(ranks[i].Vwl);
					explain.append(" &gt; ");
					explain.append(ranks[i].Vlw);
					explain.append(")<br />\n");
				}
				ranks[i].active = false;
			}
			++i;
		}
		if ( explain != null ) {
			explain.append("</p><p>Final pair rankings:</p><table border=\"0\">\n");
			for ( i = 0; i < x; ++i ) {
				if (ranks[i].active) {
					explain.append("<tr><td>");
				} else {
					explain.append("<tr style=\"color: #999999;\"><td>");
				}
				explain.append(they[ranks[i].winner].name);
				explain.append("</td><td>&gt;</td><td>");
				explain.append(they[ranks[i].loser].name);
				explain.append("</td><td>(");
				explain.append(ranks[i].Vwl);
				explain.append(" &gt; ");
				explain.append(ranks[i].Vlw);
				explain.append(")</td></tr>\n");
			}
			explain.append("</table>\n");
		}
		for ( i = 0; i < numc; ++i ) {
			defeatCount[i] = 0;
		}
		for ( i = 0; i < x; ++i ) {
			if ( ranks[i].active ) {
				defeatCount[ranks[i].loser]++;
			}
		}
		return winners = makeWinners( they, defeatCount );
	}
	
	/**
	temp data for Ranked Pairs
	*/
	protected static class SearchStack {
		public int from;
		public String fromName = null;
		public SearchStack prev;
		public SearchStack(int fromIn) {
			from = fromIn;
			prev = null;
		}
		public SearchStack(int fromIn, SearchStack prevIn) {
			from = fromIn;
			prev = prevIn;
		}
	}
	protected static final String spaces =
	"                                                                        ";
	/**
	@param ranks a > b pairs
	@param numranks use ranks[0]..ranks[numranks-1] inclusive.
	@param from find a path starting with from as winner
	@param to and path ending at to as loser
	*/
	protected boolean findPath(Pair[] ranks, int numranks, int from, int to) {
		return findPath(ranks, numranks, from, to, null);
	}
	
	protected void appendBackPath(Pair[] ranks, StringBuffer sb, SearchStack up) {
		if (up.prev != null) {
			appendBackPath(ranks, sb, up.prev);
		}
		Count[] they = getIndexedCounts(false);
		sb.append(they[up.from].name);
		sb.append(" -> ");
	}
	/**
	@param ranks a > b pairs
	@param numranks use ranks[0]..ranks[numranks-1] inclusive.
	@param from find a path starting with from as winner
	@param to and path ending at to as loser
	@param up SearchStack call with null at first. Used in recursion.
	*/
	protected boolean findPath(Pair[] ranks, int numranks, int from, int to, SearchStack up) {
		SearchStack here = new SearchStack(from, up);
		int depth = 0;  // verbose debugging feature
		String prefix = "";  // verbose debugging feature
		if ( debugLog != null ) {
			SearchStack cur = up;
			while ( cur != null ) {
				depth++;
				cur = cur.prev;
			}
			depth *= 2;
			if ( depth > spaces.length() ) { depth = spaces.length(); }
			prefix = spaces.substring( 0, depth );
			debugLog.append(prefix).append("findpath(:").append(numranks).append(") ").append(from).append("->").append(to).append("\n");
		}
		int i;
		for ( i = 0; i < numranks; ++i ) {
			if ( ! ranks[i].active ) {
				continue;
			}
			/*if ( (!ranks[i].active) &&
					(ranks[i].compareTo(ranks[numranks]) != 0) ) {
				continue;
			}*/
			if ( ranks[i].winner == from ) {
				if ( ranks[i].loser == to ) {
					if ( debugLog != null ) { debugLog.append(prefix).append("found ").append(from).append("->").append(to).append("\n"); }
					if ( (explainVerbosity >= 1) && (explain != null) ) {
						explain.append("<p>Back path: ");
						appendBackPath(ranks, explain, here);
						Count[] they = getIndexedCounts(false);
						explain.append(they[to].name);
						explain.append("</p>");
					}
					return true;
				}
				boolean beenthere = false;
				SearchStack cur = up;
				while ( cur != null ) {
					if ( cur.from == ranks[i].loser ) {
						if ( debugLog != null ) { debugLog.append(prefix).append(" beenthere: ").append(ranks[i].loser).append("\n"); }
						beenthere = true;
						break;
					}
					cur = cur.prev;
				}
				if ( ! beenthere ) {
					if ( debugLog != null ) { debugLog.append(prefix).append(" try through ").append(ranks[i].loser).append("\n"); }
					boolean maybepath = findPath(ranks, numranks, ranks[i].loser, to, here);
					if ( maybepath ) {
						if ( debugLog != null ) { debugLog.append(prefix).append(" found ").append(ranks[i].loser).append("->").append(to).append("\n"); }
						return maybepath;
					} else {
						if ( debugLog != null ) { debugLog.append(prefix).append(" fail through ").append(ranks[i].loser).append("\n"); }
					}
				}
			}
		}
		if ( debugLog != null ) {
			debugLog.append(prefix).append("findpath(:").append(numranks).append(") ").append(from).append("->").append(to).append(" false\n");
		}
		return false;
	}

	public static void countDefeats( int numc, int[] tally, int[] defeatCount ) {
		/* ndefeats is "numc choose 2" or ((numc !)/((2 !)*((numc - 2)!))) */
		int j,k;
		
		for ( j = 0; j < numc; j++ ) {
			defeatCount[j] = 0;
		}
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				int vk, vj;
				vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ( vj > vk ) {
					defeatCount[k]++;
				} else if ( vj < vk ) {
					defeatCount[j]++;
				}
			}
		}
	}

	public static int[] getSchwartzSet( int numc, int[] tally, int[] defeatCount, StringBuffer debugsb ) {
		countDefeats( numc, tally, defeatCount );
		// start out set with first choice (probabbly replace it)
		int j,k;
		int mindefeats = defeatCount[0];
		int numWinners = 1;
		int choiceIndecies[] = new int[numc];
		choiceIndecies[0] = 0;
		for ( j = 1; j < numc; j++ ) {
			if ( defeatCount[j] < mindefeats ) {
				choiceIndecies[0] = j;
				numWinners = 1;
				mindefeats = defeatCount[j];
			} else if ( defeatCount[j] == mindefeats ) {
				choiceIndecies[numWinners] = j;
				numWinners++;
			}
		}
		if ( mindefeats != 0 ) {
			// the best there is was defeated by some choice, make sure that is added to the set
			for ( int i = 0; i < numWinners; i++ ) {
				// foreach k in set of least defeated ...
				k = choiceIndecies[i];
				for ( j = 0; j < numc; j++ ) if ( k != j ) {
					int vk, vj;
					vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
					vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
					if ( vj > vk ) {
						// j defeats k, j must be in the set
						boolean gotj = false;
						for ( int si = 0; si < numWinners; si++ ) {
							if ( choiceIndecies[si] == j ) {
								gotj = true;
								break;
							}
						}
						if ( ! gotj ) {
							choiceIndecies[numWinners] = j;
							numWinners++;
						}
					}
				}
			}
		}
		int[] sset = new int[numWinners];
		for ( j = 0; j < numWinners; j++ ) {
			sset[j] = choiceIndecies[j];
		}
		if ( ! verifySchwartzSet( numc, tally, sset, debugsb ) ) {
			System.err.println("getSchwartzSet is returning an invalid Schwartz set!");
			if ( debugsb != null ) {
				htmlDebugTable( debugsb, numc, tally, "tally not met by schwartz set", null );
				debugsb.append( "bad sset: " );
				debugsb.append( sset[0] );
				for ( j = 1; j < sset.length; j++ ) {
					debugsb.append(", ");
					debugsb.append(sset[j]);
				}
			}
		}
		return sset;
	}
	/** Verify set to have Schwartz Set properties.
	 <ol><li>every member of the set beats every choice not in the set</li>
	 <li>no member of the set is beaten by every other member of the set</li></ol>
	 @param ss a candidate Schwartz Set
	 @return true if ss is a Schwartz Set */
	public static boolean verifySchwartzSet( int numc, int[] tally, int[] ss, StringBuffer debugsb ) {
		for ( int i = 0; i < ss.length; i++ ) {
			int m;
			m = ss[i];
			// check for defeats by choices outside the set
			for ( int j = 0; j < numc; j++ ) {
				boolean notinset;
				notinset = true;
				for ( int k = 0; k < ss.length; k++ ) {
					if ( ss[k] == j ) {
						notinset = false;
						break;
					}
				}
				if ( notinset ) {
					int vm, vj;
					vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
					vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
					if ( vj > vm ) {
						if ( debugsb != null ) {
							debugsb.append("choice ");
							debugsb.append(m);
							debugsb.append(" in bad schwartz set defeated by ");
							debugsb.append(j);
							debugsb.append(" not in set\n");
						}
						return false;
					}
				}
			}
			// check if defated by all choices inside the set
			int innerDefeats = 0;
			for ( int k = 0; k < ss.length; k++ ) {
				int j;
				j = ss[k];
				if ( m != j ) {
					int vm, vj;
					vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
					vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
					if ( vj > vm ) {
						innerDefeats++;
					}
				}
			}
			if ( (innerDefeats > 0) && (innerDefeats == ss.length - 1) ) {
				if ( debugsb != null ) {
					debugsb.append("choice ");
					debugsb.append(m);
					debugsb.append(" in bad schwartz is defeated by all in set.\n");
				}
				return false;
			}
		}
		// not disproven by exhaustive test, thus it's good
		return true;
	}

	/**
	 Simplified debug table printing based on just Count[].
	 @param sb gets text appended to it. Must not be null.
	 @param they internal state from getIndexedCounts()
	 @param title String to go in a titular place in a HTML table. May be null.
	 @return the passed in StringBuffer sb, with stuff appended.
	 @see #getIndexedCounts
	 */
	public static StringBuffer htmlDebugTable( StringBuffer sb, Count they[], String title ) {
		return htmlDebugTable( sb, they.length, getTallyArray( they ), title, they );
	}

	/**
	 Print a table of intermediate state.
	 @param sb gets text appended to it. Must not be null.
	 @param numc how many choices in the world to consider
	 @param arr likely was once the result of getTallyArray().
	 @param they internal state from getIndexedCounts()
	 @param title String to go in a titular place in a HTML table. May be null.
	 @return the passed in StringBuffer sb, with stuff appended.
	 @see #getTallyArray
	 */
	public static StringBuffer htmlDebugTable(
			StringBuffer sb, int numc, int[] arr,
			String title, Count they[] ) {
		if ( they != null ) {
			sb.append( "<table border=\"1\"><tr><th></th><th colspan=\"" );
			if ( arr == null ) {
				arr = getTallyArray( they );
			}
		} else {
			sb.append( "<table border=\"1\"><tr><th>Choice Index</th><th colspan=\"");
		}
		sb.append( numc );
		sb.append("\">");
		if ( title != null ) {
			sb.append( title );
		}
		sb.append("</th></tr>\n" );
		for ( int i = 0; i < numc; i++ ) {
			sb.append("<tr><td>");
			if ( they != null ) {
				sb.append( they[i].name );
			} else {
				sb.append( i );
			}
			sb.append("</td>");
			for ( int j = 0; j < numc; j++ ) {
				if ( (i == j) && (arr[i*numc + j] == 0) ) {
					sb.append("<td bgcolor=\"#ffffff\"></td>");
				} else {
					if ( arr[i*numc + j] > arr[j*numc + i] ) {
						sb.append("<td bgcolor=\"#bbffbb\">");
					} else if ( arr[i*numc + j] < arr[j*numc + i] ) {
						sb.append("<td bgcolor=\"#ffbbbb\">");
					} else {
						sb.append("<td>");
					}
					sb.append(arr[i*numc + j]);
					sb.append("</td>");
				}
			}
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");
		return sb;
	}
	
	public StringBuffer htmlTable(StringBuffer sb, boolean css) {
		return htmlTable(sb, getWinners(), getIndexedCounts( debug ), css);
	}
	public static StringBuffer htmlTable(
			StringBuffer sb, NameVote[] winners, Count[] they, boolean css) {
		if (css) {
			sb.append( "<table class=\"v_vrrt\"><tr><td colspan=\"2\"></td>" );
		} else {
			sb.append( "<table border=\"1\"><tr><td colspan=\"2\"></td>" );
		}
		for ( int i = 0; i < winners.length; i++ ) {
			sb.append( "<th>" );
			sb.append( i+1 );
			sb.append( "</th>" );
		}
		sb.append( "</tr>" );
		int[] indextr = new int[winners.length];
		for ( int xi = 0; xi < winners.length; xi++ ) {
			for ( int i = 0; i < they.length; i++ ) {
				if ( winners[xi].name.equals( they[i].name ) ) {
					indextr[xi] = i;
				}
			}
		}
		for ( int xi = 0; xi < indextr.length; xi++ ) {
			int i;
			i = indextr[xi];
			sb.append( "<tr><th>" );
			sb.append( xi+1 );
			sb.append( "</th><td>" );
			sb.append( they[i].name );
			sb.append( "</td>" );
			for ( int xj = 0; xj < indextr.length; xj++ ) {
				int j;
				j = indextr[xj];
				if ( i == j ) {
					sb.append( "<td></td>" );
				} else {
					int thisw, otherw;
					if ( i > j ) {
						thisw = they[i].counts[j];
						otherw = they[i].counts[j + i];
					} else /*if ( i < j )*/ {
						thisw = they[j].counts[j + i];
						otherw = they[j].counts[i];
					}
					if ( thisw > otherw ) {
						if (css) {
							sb.append("<td class=\"v_vrrhi\">");
						} else {
							sb.append("<td bgcolor=\"#bbffbb\">");
						}
					} else if ( thisw < otherw ) {
						if (css) {
							sb.append("<td class=\"v_vrrlo\">");
						} else {
							sb.append("<td bgcolor=\"#ffbbbb\">");
						}
					} else {
						sb.append("<td>");
					}				
					sb.append( thisw );
					sb.append( "</td>" );
				}
			}
			sb.append( "</tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	/** Tempate key.
	@see #beatByBlock(StringBuffer,String[] template) */
	public static final String HIGHER_NAME = "HIGHER_NAME";
	/** Tempate key.
	@see #beatByBlock(StringBuffer,String[] template) */
	public static final String LOWER_NAME = "LOWER_NAME";
	/** Tempate key.
	@see #beatByBlock(StringBuffer,String[] template) */
	public static final String HIGHER_COUNT = "HIGHER_COUNT";
	/** Tempate key.
	@see #beatByBlock(StringBuffer,String[] template) */
	public static final String LOWER_COUNT = "LOWER_COUNT";
	/**
	Used by htmlBeatByBlock.
	&lt;div class="v_bbr"&gt;&lt;span class="v_hi"&gt;{{HIGHER_NAME}}&lt;/span&gt; was preferred over &lt;span class="v_lo"&gt;{{LOWER_NAME}}&lt;/span&gt; by {{HIGHER_COUNT}} voters. {{LOWER_COUNT}} voters had the reverse preference.&lt;/div&gt;
	@see #htmlBeatByBlock(StringBuffer)
	*/
	// "<div class=\"v_bbr\"><span class=\"v_hi\">{{HIGHER_NAME}}</span> was preferred over <span class=\"v_lo\">{{LOWER_NAME}}</span> by {{HIGHER_COUNT}} voters. {{LOWER_COUNT}} voters had the reverse preference.</div>"
	public static final String[] kDefaultBeatByBlockTemplate = {
		"<div class=\"v_bbr\"><span class=\"v_hi\">",
		HIGHER_NAME,
		"</span> was preferred over <span class=\"v_lo\">",
		LOWER_NAME,
		"</span> by ",
		HIGHER_COUNT,
		" voters. ",
		LOWER_COUNT,
		" voters had the reverse preference.</div>"
	};
	/**
	Print collection of statements of the form "a was preferred over b by N voters. M preferred the other way".
	The exact html template is:<br>
	&lt;div class="v_bbr"&gt;&lt;span class="v_hi"&gt;{{HIGHER_NAME}}&lt;/span&gt; was preferred over &lt;span class="v_lo"&gt;{{LOWER_NAME}}&lt;/span&gt; by {{HIGHER_COUNT}} voters. {{LOWER_COUNT}} voters had the reverse preference.&lt;/div&gt;
	*/
	public StringBuffer htmlBeatByBlock(StringBuffer sb) {
		return beatByBlock(sb, kDefaultBeatByBlockTemplate);
	}
	/**
	Print collection of statements of the form "a was preferred over b by N voters. M preferred the other way".
	This statement can be templatized by passing in an array of string fragments and keys {HIGHER_NAME, LOWER_NAME, HIGHER_COUNT, LOWER_COUNT}.
	@param template is a collection of string fragments
	*/
	public StringBuffer beatByBlock(StringBuffer sb, String[] template) {
		if (getWinners() == null) {
			return sb;
		}
		Count[] counts = getIndexedCounts( debug );
		// indextr converts winners indecies into counts indecies
		int[] indextr = new int[winners.length];
		for ( int xi = 0; xi < winners.length; xi++ ) {
			for ( int i = 0; i < counts.length; i++ ) {
				if ( winners[xi].name.equals( counts[i].name ) ) {
					indextr[xi] = i;
				}
			}
		}
		int xi = 0;
		int i = indextr[xi];
		for ( int xj = 0; xj < indextr.length; xj++ ) {
			if (xj == xi) {
				continue;
			}
			int j = indextr[xj];
			int vij, vji;
			if ( i > j ) {
				vij = counts[i].counts[j];
				vji = counts[i].counts[j + i];
			} else /*if ( i < j )*/ {
				vij = counts[j].counts[j + i];
				vji = counts[j].counts[i];
			}
			for (int t = 0; t < template.length; t++) {
				if (template[t].equals(HIGHER_NAME)) {
					if (vij >= vji) {
						sb.append(counts[i].name);
					} else {
						sb.append(counts[j].name);
					}
				} else if (template[t].equals(LOWER_NAME)) {
					if (vij >= vji) {
						sb.append(counts[j].name);
					} else {
						sb.append(counts[i].name);
					}
				} else if (template[t].equals(HIGHER_COUNT)) {
					if (vij >= vji) {
						sb.append(vij);
					} else {
						sb.append(vji);
					}
				} else if (template[t].equals(LOWER_COUNT)) {
					if (vij >= vji) {
						sb.append(vji);
					} else {
						sb.append(vij);
					}
				} else {
					sb.append(template[t]);
				}
			}
		}
		return sb;
	}

	static {
		registerImpl( "VRR", VRR.class );
	}
};
