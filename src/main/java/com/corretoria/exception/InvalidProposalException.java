package com.corretoria.exception;

/**
 * Proposta incoerente — traduzida em {@code 400} com {@code /problems/invalid-proposal}.
 *
 * <p>Irmã de {@link InvalidListingException} e {@link InvalidSavedSearchException}: os campos
 * são válidos, a combinação é que não existe no domínio — propor sobre o próprio anúncio,
 * abrir negociação sobre anúncio fora do ar, decidir uma proposta já decidida.
 */
public class InvalidProposalException extends RuntimeException {

    public InvalidProposalException(String message) {
        super(message);
    }

    public static InvalidProposalException cannotProposeOnOwnListing() {
        return new InvalidProposalException(
                "Não é possível propor sobre o próprio anúncio");
    }

    public static InvalidProposalException listingIsNotActive() {
        return new InvalidProposalException(
                "Só anúncios ativos aceitam proposta");
    }

    public static InvalidProposalException savedSearchRequiredForRealtor() {
        return new InvalidProposalException(
                "Ao propor a partir da vitrine, informe em \"savedSearchId\" a busca pública "
                        + "que originou a abordagem");
    }

    public static InvalidProposalException savedSearchNotAllowedForCustomer() {
        return new InvalidProposalException(
                "\"savedSearchId\" só faz sentido quando é o corretor que toma a iniciativa");
    }

    public static InvalidProposalException savedSearchIsNotPublic() {
        return new InvalidProposalException(
                "Só busca pública e ativa autoriza a abordagem do corretor");
    }

    public static InvalidProposalException alreadyDecided() {
        return new InvalidProposalException(
                "A proposta já foi decidida: aceitar, recusar e retirar são definitivos. "
                        + "Para retomar, abra uma proposta nova");
    }

    public static InvalidProposalException cannotReturnToPending() {
        return new InvalidProposalException(
                "PENDING é o estado inicial da proposta, não um desfecho");
    }
}
