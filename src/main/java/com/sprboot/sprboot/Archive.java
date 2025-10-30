package com.sprboot.sprboot;

public class Archive {

    public void validatingExistence() {

        // validate productInstanceID
        /*
         * List<Long> ids = addShipmentRequest.getProductInstanceID();
         * 
         * List<ProductInstance> foundInstances =
         * productInstanceRepository.findAllById(ids); // find all productinstances
         * // with one query
         * 
         * if (foundInstances.size() != ids.size()) {
         * List<Long> foundIds =
         * foundInstances.stream().map(ProductInstance::getInstanceID).toList();
         * List<Long> missingIds = ids.stream().filter(id ->
         * !foundIds.contains(id)).toList();
         * throw new IllegalArgumentException("Missing Product Instance ID: " +
         * missingIds);
         * }
         */
    }
}
