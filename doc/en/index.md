# GeoServer Documentation

Welcome to the GeoServer documentation. Choose a guide to get started:

<div class="doc-cards">
  <a href="user/" class="doc-card">
    <div class="doc-card-icon">📖</div>
    <h2>User Manual</h2>
    <p>Learn how to install, configure, and use GeoServer for publishing geospatial data</p>
  </a>

  <a href="developer/" class="doc-card">
    <div class="doc-card-icon">💻</div>
    <h2>Developer Guide</h2>
    <p>Contribute to GeoServer development with architecture guides and coding standards</p>
  </a>

  <a href="docguide/" class="doc-card">
    <div class="doc-card-icon">✍️</div>
    <h2>Documentation Guide</h2>
    <p>Learn how to write and contribute to GeoServer documentation</p>
  </a>

  <a href="api/" class="doc-card">
    <div class="doc-card-icon">🔌</div>
    <h2>API Reference</h2>
    <p>Explore the REST API and service endpoints for programmatic access</p>
  </a>
</div>

<style>
.doc-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  margin: 3rem 0;
  padding: 0;
}

.doc-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2.5rem 2rem;
  border: 2px solid var(--md-default-fg-color--lightest);
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s ease;
  background: var(--md-default-bg-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.doc-card:hover {
  border-color: var(--md-primary-fg-color);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}

.doc-card-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
  line-height: 1;
}

.doc-card h2 {
  margin: 0.5rem 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--md-primary-fg-color);
}

.doc-card p {
  margin: 0.75rem 0 0 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--md-default-fg-color--light);
}

/* Dark mode adjustments */
[data-md-color-scheme="slate"] .doc-card {
  border-color: var(--md-default-fg-color--lighter);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

[data-md-color-scheme="slate"] .doc-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

/* Mobile responsive */
@media screen and (max-width: 768px) {
  .doc-cards {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    margin: 2rem 0;
  }
  
  .doc-card {
    padding: 2rem 1.5rem;
  }
  
  .doc-card-icon {
    font-size: 3rem;
  }
  
  .doc-card h2 {
    font-size: 1.3rem;
  }
}
</style>
