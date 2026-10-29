package com.corretoria.service;

import com.corretoria.vo.shared.PhotoContent;

/**
 * Porta de armazenamento de objetos.
 *
 * <p>A abstração existe para que a regra de negócio do anúncio dependa de "gravar uma foto e
 * receber a chave", e não do SDK da AWS. Trocar MinIO por S3 gerenciado — ou por um duplo em
 * teste — é trocar a implementação, sem tocar no {@code ListingService}.
 */
public interface StorageService {

    /**
     * Grava o conteúdo e devolve a chave do objeto.
     *
     * <p>A chave é gerada aqui, nunca recebida do cliente: nome de arquivo enviado pelo
     * usuário permitiria sobrescrever objeto alheio ou escapar do prefixo.
     *
     * @param photo     conteúdo já validado
     * @param keyPrefix prefixo lógico do objeto, tipicamente {@code listings/{id}}
     * @return chave do objeto gravado, a ser persistida em {@code ListingPhoto.storageKey}
     */
    String store(PhotoContent photo, String keyPrefix);

    /** Remove o objeto. Chave inexistente não é erro — a operação é idempotente. */
    void delete(String storageKey);

    /**
     * Prefixo público de leitura dos objetos, sem barra final.
     *
     * <p>A URL de uma foto é {@code baseUrl() + "/" + storageKey}. Só o prefixo é exposto
     * porque a montagem final acontece no mapeador, que não pode depender desta camada.
     */
    String baseUrl();
}
