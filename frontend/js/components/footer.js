// componente do rodape com links, contato e redes sociais
export function Footer() {
  return `
    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section">
            <div class="brand">
              <div class="logo">
                <i class="fas fa-laptop-code"></i>
              </div>
              <div class="brand-text">
                <div class="brand-name">Technomade</div>
                <div class="brand-tagline">Digital Nomad</div>
              </div>
            </div>
            <p class="footer-description">
              Os melhores equipamentos para nômades digitais que trabalham de qualquer lugar do mundo.
            </p>
          </div>

          <div class="footer-section">
            <h4>Links Rápidos</h4>
            <ul>
              <li><a href="#" onclick="navigate('home'); return false;">Home</a></li>
              <li><a href="#" onclick="navigate('products'); return false;">Produtos</a></li>
              <li><a href="#" onclick="navigate('cart'); return false;">Carrinho</a></li>
            </ul>
          </div>

          <div class="footer-section">
            <h4>Contato</h4>
            <ul>
              <li><i class="fas fa-envelope"></i> contato@technomade.com</li>
              <li><i class="fas fa-phone"></i> (11) 99999-9999</li>
              <li><i class="fas fa-map-marker-alt"></i> São Paulo, SP</li>
            </ul>
          </div>

          <div class="footer-section">
            <h4>Redes Sociais</h4>
            <div class="social-links">
              <a href="#"><i class="fab fa-instagram"></i></a>
              <a href="#"><i class="fab fa-facebook"></i></a>
              <a href="#"><i class="fab fa-twitter"></i></a>
            </div>
          </div>
        </div>

        <div class="footer-bottom">
          <p>&copy; 2025 Technomade. Todos os direitos reservados.</p>
        </div>
      </div>
    </footer>
  `;
}
