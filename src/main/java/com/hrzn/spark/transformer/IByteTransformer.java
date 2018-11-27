package com.hrzn.spark.transformer;

/**
 * A bytecode transformer acting on raw class bytes
 */
public interface IByteTransformer {
    /**
     * Transforms the given class bytes.
     *
     * @param target the fully qualified class name being transformed
     * @param bytes the input bytes of this class
     * @return transformed bytes for this class
     */
    byte[] transform(String target, byte[] bytes);
}
