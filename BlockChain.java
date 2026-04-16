import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   // added these fields to reference the block chain and transaction pool
   private HashMap<ByteArrayWrapper, BlockNode> blockChain;
   private TransactionPool txPool;
   private BlockNode maxHeightNode;

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
   }

   /*
    * create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */
   public BlockChain(Block genesisBlock) {

      // IMPLEMENT THIS
      blockChain = new HashMap<>();
      txPool = new TransactionPool();

      UTXOPool genesisPool = new UTXOPool();

      Transaction coinbase = genesisBlock.getCoinbase();
      for (int i = 0; i < coinbase.numOutputs(); i++) {
         UTXO utxo = new UTXO(coinbase.getHash(), i);
         genesisPool.addUTXO(utxo, coinbase.getOutput(i));
      }

      BlockNode genesisNode = new BlockNode(genesisBlock, null, genesisPool);
      blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
      maxHeightNode = genesisNode;
   }

   /*
    * Get the maximum height block
    */
   public Block getMaxHeightBlock() {
      // IMPLEMENT THIS (DONE)
      return maxHeightNode.b;

   }

   /*
    * Get the UTXOPool for mining a new block on top of
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      // IMPLEMENT THIS (DONE)
      return maxHeightNode.getUTXOPoolCopy();
   }

   /*
    * Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
      // IMPLEMENT THIS (DONE)
      return txPool;
   }

   /*
    * Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1.
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height
    * 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      // IMPLEMENT THIS
      if (b == null)
         return false;

      byte[] prevHash = b.getPrevBlockHash();
      if (prevHash == null)
         return false;

      BlockNode parentNode = blockChain.get(new ByteArrayWrapper(prevHash));
      if (parentNode == null)
         return false;

      int newHeight = parentNode.height + 1;

      if (newHeight <= maxHeightNode.height - CUT_OFF_AGE) {
         return false;
      }

      UTXOPool parentPoolCopy = parentNode.getUTXOPoolCopy();
      TxHandler handler = new TxHandler(parentPoolCopy);

      Transaction[] txs = new Transaction[b.getTransactions().size()];
      for (int i = 0; i < b.getTransactions().size(); i++) {
         txs[i] = b.getTransactions().get(i);
      }

      Transaction[] validTxs = handler.handleTxs(txs);

      if (validTxs.length != txs.length) {
         return false;
      }

      UTXOPool newPool = handler.getUTXOPool();

      Transaction coinbase = b.getCoinbase();
      for (int i = 0; i < coinbase.numOutputs(); i++) {
         UTXO utxo = new UTXO(coinbase.getHash(), i);
         newPool.addUTXO(utxo, coinbase.getOutput(i));
      }

      BlockNode newNode = new BlockNode(b, parentNode, newPool);
      blockChain.put(new ByteArrayWrapper(b.getHash()), newNode);

      if (newNode.height > maxHeightNode.height) {
         maxHeightNode = newNode;
      }

      for (Transaction tx : txs) {
         txPool.removeTransaction(tx.getHash());
      }

      return true;
   }

   /*
    * Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
      // IMPLEMENT THIS (DONE)
      txPool.addTransaction(tx);
   }

}