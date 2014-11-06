package layers

import datatypes.index_node
import datatypes.key
import datatypes.znode
import helpers.scala.MapWrapperDeep
import helpers.scala.Ref
import misc.default_znode
import misc.address
import misc.MIN_SIZE
import misc.BRANCH_SIZE
import misc.ADR_DUMMY

class BtreeRec(private var ROOT: znode, private val FS: MapWrapperDeep[address, index_node]) extends BtreeBase(ROOT, FS) {
  def this() = this(default_znode, new MapWrapperDeep[address, index_node])
  override def insert(KEY: key, ADR: address) {
    val FOUND = new Ref[Boolean](false)
    val R = new Ref[znode](ROOT)
    val ADR0 = new Ref[address](misc.uninit_address())
    lookup_loop(KEY, R.get, ADR0.get, FOUND.get)
    if (FOUND.get != true) {
      insert_rec(R.get, null, KEY, ADR)
    }
  }
  override def delete(KEY: key) {
    val FOUND = new Ref[Boolean](false)
    val R = new Ref[znode](null)
    val ADR = new Ref[address](misc.uninit_address())
    lookup_loop(KEY, R.get, ADR.get, FOUND.get)
    if (FOUND.get) {
      delete_rec(R.get, KEY)
    }
  }
  override def lookup(KEY: key, ADR: Ref[address], FOUND: Ref[Boolean]) = ???

  private def insert_rec(R: znode, CHILD: znode, KEY: key, ADR: address) {
    if (R.usedsize < BRANCH_SIZE) {
      insert_branch(R, CHILD, KEY, ADR)
    } else {
      val R0 = new Ref[znode](null)
      split(R, CHILD, KEY, ADR, R0.get)
      if (R.parent != null) {
        if (R0.get.leaf) {
          insert_rec(R.parent, R0.get, R.zbranches(R.usedsize - 1).key, ADR_DUMMY)
        } else {
          insert_rec(R.parent, R0.get, R0.get.zbranches(0).key, ADR_DUMMY)
        }
      }
    }
  }
  private def delete_rec(R: znode, KEY: key) {
    delete_branch(R, KEY)
    if (R.parent != null && R.usedsize < MIN_SIZE) {
      var DONE: Boolean = false
      val POS = new Ref[Int](0)
      getPosition(R, POS.get)
      if (POS.get > 0) {
        check_branch(R.parent, POS.get - 1)
        if (R.parent.zbranches(POS.get - 1).child.usedsize >= MIN_SIZE + 1) {
          move_from_left(R, R.parent.zbranches(POS.get - 1).child)
          DONE = true
        }
      }
      if (DONE != true && POS.get + 1 < R.parent.usedsize) {
        check_branch(R.parent, POS.get + 1)
        if (R.parent.zbranches(POS.get + 1).child.usedsize >= MIN_SIZE + 1) {
          move_from_right(R, R.parent.zbranches(POS.get + 1).child)
          DONE = true
        }
      }
      if (DONE != true) {
        if (POS.get > 0) {
          merge(R.parent.zbranches(POS.get - 1).child, R)
          if (R.parent != null) {
            delete_rec(R.parent, R.parent.zbranches(POS.get).key)
          }
        } else {
          merge(R, R.parent.zbranches(POS.get + 1).child)
          if (R.parent != null) {
            delete_rec(R.parent, R.parent.zbranches(POS.get + 1).key)
          }
        }
        DONE = true
      }
    }
  }
}