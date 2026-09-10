package com.example.demo.Serviços;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.ConsultasBD.AreaTrabalhoRepository;
import com.example.demo.ConsultasBD.ParticipacaoAreaRepository;
import com.example.demo.ConsultasBD.TokenRepository;
import com.example.demo.Entidades.AreaTrabalho;
import com.example.demo.Entidades.ParticipacaoArea;
import com.example.demo.Entidades.PermissaoArea;
import com.example.demo.Entidades.Token;
import com.example.demo.Entidades.Usuario;

// Aceita um link de convite genérico (compartilhado por "Copiar Link"), diferente do
// convite por e-mail: não é de uso único e não é amarrado a um destinatário específico —
// qualquer usuário que abrir o link (e estiver logado) entra na área com o nível de
// permissão escolhido na hora de gerar o link.
@Service
public class ConviteAreaService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AreaTrabalhoRepository areaTrabalhoRepository;

    @Autowired
    private ParticipacaoAreaRepository participacaoAreaRepository;

    public record Resultado(boolean sucesso, String mensagem, AreaTrabalho area) {}

    @Transactional
    public Resultado aceitarLinkGenerico(String tokenString, Usuario usuario) {
        Token token = tokenRepository.findByToken(tokenString);

        if (token == null || token.getAreaId() == null || token.getEmail() != null) {
            return new Resultado(false, "Link de convite inválido.", null);
        }

        if (token.getExpiraEm().isBefore(LocalDateTime.now())) {
            return new Resultado(false, "Link de convite expirado.", null);
        }

        AreaTrabalho area = areaTrabalhoRepository.findById(token.getAreaId()).orElse(null);
        if (area == null) {
            return new Resultado(false, "Área de trabalho não encontrada.", null);
        }

        boolean jaParticipa = area.getParticipacoes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuario.getId()));

        if (!jaParticipa) {
            PermissaoArea permissao = token.getPermissao() != null ? token.getPermissao() : PermissaoArea.EDITOR;
            participacaoAreaRepository.save(new ParticipacaoArea(usuario, area, permissao));
        }

        return new Resultado(true, "Você agora participa da área \"" + area.getNome() + "\".", area);
    }
}
