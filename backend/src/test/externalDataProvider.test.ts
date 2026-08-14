import { CatalogService } from "../services/catalogService.js";
import { IExternalDataProvider, NormalizedData } from "../providers/externalDataProvider.js";

class FailingProvider implements IExternalDataProvider {
  public readonly name = "FailingProvider";
  public async fetchNormalizedData(): Promise<NormalizedData | null> {
    return null;
  }
}

class MockSuccessProvider implements IExternalDataProvider {
  public readonly name = "MockSuccessProvider";
  public async fetchNormalizedData(): Promise<NormalizedData | null> {
    return {
      banners: [
        { id: "b1", title: "Mock Banner", subtitle: "Test", imageUrl: "http://localhost:3000/assets/images/banners/banner_fashion.jpg" }
      ],
      categories: [
        { id: "c1", name: "Mock Category", imageUrl: "http://localhost:3000/assets/images/categories/cat_women.jpg", subcategories: [] }
      ],
      products: [
        {
          id: "p1",
          title: "Mock Product",
          description: "Test Description",
          price: 100,
          imageUrl: "http://localhost:3000/assets/images/products/shawl_cover.jpg",
          galleryImages: [],
          rating: 4.5,
          reviewCount: 5,
          categoryId: "c1",
          isAvailable: true,
          availableQuantity: 10
        }
      ]
    };
  }
}

async function runProviderTests() {
  console.log("--- Starting ExternalDataProvider & Fallback Verification Suite ---");

  // 1. Success Provider test
  const successService = new CatalogService(new MockSuccessProvider());
  await successService.initialize();

  const homeData = successService.getHomeData();
  if (homeData.featuredProducts.length !== 1 || homeData.featuredProducts[0].id !== "p1") {
    throw new Error("MockSuccessProvider failed to initialize catalog");
  }
  console.log("  MockSuccessProvider Test PASSED");

  // 2. Failing Provider Fallback test (should fallback to Cache or Local Seed Data)
  const failingService = new CatalogService(new FailingProvider());
  await failingService.initialize();

  const fallbackCategories = failingService.getCategories();
  const fallbackProducts = failingService.getProducts();

  if (fallbackCategories.length === 0 || fallbackProducts.length === 0) {
    throw new Error("FailingProvider fallback chain failed to provide catalog data");
  }
  console.log(`  FailingProvider Fallback Test PASSED (${fallbackProducts.length} products loaded via fallback)`);

  console.log("--- ALL EXTERNAL DATA PROVIDER TESTS PASSED! ---");
}

runProviderTests().catch((err) => {
  console.error("External data provider test failed:", err);
  process.exitCode = 1;
});
